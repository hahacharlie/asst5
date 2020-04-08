package visitor;

import syntaxtree.*;

import errorMsg.*;
import java.io.*;

public class CG3Visitor extends ASTvisitor {

	// the purpose here is to annotate things with their offsets:
	// - formal parameters, with respect to the (callee) frame
	// - local variables, with respect to the frame
	// - instance variables, with respect to their slot in the object
	// - methods, with respect to their slot in the v-table
	// - while statements, with respect to the stack-size at the time
	//   of loop-exit
	
	// error message object
	private ErrorMsg errorMsg;
	
	// IO stream to which we will emit code
	private CodeStream code;

	// current stack height
	private int stackHeight;
	
	// for constant evaluation
	private ConstEvalVisitor conEvalVis;
	
	public CG3Visitor(ErrorMsg e, PrintStream out) {
		initInstanceVars(e, out);
		conEvalVis = new ConstEvalVisitor();
	}
	
	private void initInstanceVars(ErrorMsg e, PrintStream out) {
		errorMsg = e;
		code = new CodeStream(out, errorMsg);
		stackHeight = 0;
	}

    @Override
    public Object visitIntegerLiteral(IntegerLiteral n) {
        code.emit(n, "subu $sp,$sp,8");
        stackHeight -= 8;
        code.emit(n, "sw $s5,4($sp)");
        code.emit(n,"li $t0,"+n.val);
        code.emit(n, "sw $t0,($sp)");
	    return null;
    }

    @Override
    public Object visitNull(Null n) {
        code.emit(n,"subu $sp,$sp,4");
        stackHeight -= 4;
        code.emit(n, "sw $zero,($sp)");
	    return null;
    }

    @Override
    public Object visitTrue(True n) {
        code.emit(n, "subu $sp,$sp,4");
        stackHeight -= 4;
        code.emit(n, "li $t0,1");
        code.emit(n, "sw $t0,($sp)");
	    return null;
    }

    @Override
    public Object visitFalse(False n) {
        code.emit(n,"subu $sp,$sp,4");
        stackHeight -= 4;
        code.emit(n, "sw $zero,($sp)");
	    return null;
    }

    @Override
    public Object visitStringLiteral(StringLiteral n) {
        code.emit(n, "subu $sp,$sp,4");
        stackHeight -= 4;
        code.emit(n, "la $t0,strLit_"+n.uniqueCgRep);
        code.emit(n, "sw $t0,($sp)");
	    return null;
    }

    @Override
    public Object visitThis(This n) {
        code.emit(n, "subu $sp,$sp,4");
        stackHeight -= 4;
        code.emit(n, "sw $s2,($sp)");
	    return null;
    }

    @Override
    public Object visitSuper(Super n) {
        code.emit(n, "subu $sp,$sp,4");
        stackHeight -= 4;
        code.emit(n, "sw $s2,($sp)");
	    return null;
    }

    @Override
    public Object visitIdentifierExp(IdentifierExp n) {
        if (n.link instanceof InstVarDecl) {
            // determine variable's offset?
            code.emit(n, "lw $t0,"+n.pos+"($s2"); //not sure if this is what he meant
        }else {
            stackHeight += n.pos;
            code.emit(n, "lw $t0,"+n.pos+"($sp)");
        }
        if (n.type instanceof IntegerType) {
            code.emit(n, "subu $sp,$sp,8");
            stackHeight -= 8;
            code.emit(n, "sw $s5,4($sp)");
            code.emit(n, "sw $t0,($sp)");
        }else {
            code.emit(n, "subu $sp,$sp,4");
            stackHeight -= 4;
            code.emit(n, "sw $t0,($sp)");
        }
	    return null;
    }

    @Override
    public Object visitNot(Not n) {
        n.exp.accept(this);
        code.emit(n, "lw $t0,($sp)");
        code.emit(n, "xor $t0,$t0,1");
        code.emit(n, "sw $t0,($sp)");
	    return null;
    }

    @Override
    public Object visitPlus(Plus n) {
        n.left.accept(this); //traverse left-expression
        n.right.accept(this); //traverse right-expression
        //emit code
        code.emit(n, "lw $t0,($sp)");
        code.emit(n, "lw $t1,8($sp)");
        code.emit(n, "addu $t0,$t0,$t1");
        code.emit(n, "addu $sp,$sp,8");
        stackHeight -= 8; //subtract 8 from stackHeight
        code.emit(n, "sw $t0,($sp)");
	    return null;
    }

    @Override
    public Object visitMinus(Minus n) {
        n.left.accept(this); //traverse left-expression
        n.right.accept(this); //traverse right-expression
        //emit code
        code.emit(n, "lw $t0,($sp)");
        code.emit(n, "lw $t1,8($sp)");
        code.emit(n, "subu $t0,$t0,$t1");
        code.emit(n, "subu $sp,$sp,8");
        stackHeight -= 8; //subtract 8 from stackHeight
        code.emit(n, "sw $t0,($sp)");
        return null;
    }

    @Override
    public Object visitTimes(Times n) {
        n.left.accept(this);
        n.right.accept(this);
        code.emit(n, "lw $t0,($sp)");
        code.emit(n, "lw $t1,8($sp)");
        code.emit(n, "mult $t0,$t1");
        code.emit(n, "mflo $t0");
        code.emit(n, "addu $sp,$sp,8");
        stackHeight -= 8;
        code.emit(n, "sw $t0,($sp)");
	    return null;
    }

    @Override
    public Object visitDivide(Divide n) {
        n.left.accept(this);
        n.right.accept(this);
        code.emit(n, "jal divide");
	    return null;
    }

    @Override
    public Object visitRemainder(Remainder n) {
        n.left.accept(this);
        n.right.accept(this);
        code.emit(n, "jal remainder");
	    return null;
    }

    @Override
    public Object visitEquals(Equals n) {
        n.left.accept(this);
        n.right.accept(this);
        if (n.type instanceof IntegerType) {
            code.emit(n, "lw $t0,($sp)");
            code.emit(n, "lw $t1,8($sp)");
            code.emit(n, "seq $t0,$t0,$t1");
            code.emit(n, "addu $sp,$sp,12");
            stackHeight -= 12;
            code.emit(n, "sw $t0,($sp)");
        } else {
            code.emit(n, "lw $t0,($sp)");
            code.emit(n, "lw $t1,4($sp)");
            code.emit(n, "seq $t0,$t0,$t1");
            code.emit(n, "addu $sp,$sp,4");
            stackHeight -= 4;
            code.emit(n, "sw $t0,($sp)");
        }
	    return null;
    }

    @Override
    public Object visitGreaterThan(GreaterThan n) {
        n.left.accept(this);
        n.right.accept(this);
        code.emit(n, "lw $t0,($sp)");
        code.emit(n, "lw $t1,8($sp)");
        code.emit(n, "sgt $t0,$t1,$t0");
        code.emit(n, "addu $sp,$sp,12");
        stackHeight -= 12;
        code.emit(n, "sw $t0,($sp)");
	    return null;
    }

    @Override
    public Object visitAnd(And n) {
        n.left.accept(this);
        code.emit(n, "lw $t0,($sp)");
        code.emit(n, "beq $t0,$zero,skip_"+n.uniqueId);
        code.emit(n, "addu $sp,$sp,4");
        stackHeight -= 4;
        n.right.accept(this);
        code.emit(n, "skip_"+n.uniqueId+":");
	    return null;
    }

    @Override
    public Object visitArrayLength(ArrayLength n) {
        n.exp.accept(this);
        code.emit(n, "lw $t0, ($sp)");
        code.emit(n, "beq $t0,$zero,nullPtrException");
        code.emit(n, "lw $t0,-4($t0)");
        code.emit(n, "sw $s5,($sp)");
        code.emit(n, "subu $sp,4");
        stackHeight -= 4;
        code.emit(n, "sw $t0,($sp)");
	    return null;
    }

    @Override
    public Object visitArrayLookup(ArrayLookup n) {
	    n.arrExp.accept(this);
	    n.idxExp.accept(this);
        code.emit(n, "lw $t0,8($sp)");
        code.emit(n, "beq $t0,$zero,nullPtrException");
        code.emit(n, "lw $t1,-4($t0)");
        code.emit(n, "lw $t2,($sp)");
        code.emit(n, "bgeu $t2,$t1,arrayIndexOutOfBounds");
        code.emit(n, "sll $t2,$t2,2");
        code.emit(n, "addu $t2,$t2,$t0");
        code.emit(n, "lw $t0,($t2)");
        if (n.arrExp.type instanceof IntegerType) {
            code.emit(n, "sw $t0,4($sp)");
            code.emit(n, "sw $s5,8($sp)");
            code.emit(n, "addu $sp,$sp,4");
            stackHeight -= 4;
        } else {
            code.emit(n, "sw $t0,8($sp)");
            code.emit(n, "addu $sp,$sp,8");
            stackHeight -= 8;
        }
        return super.visitArrayLookup(n);
    }

    @Override
    public Object visitInstVarAccess(InstVarAccess n) {
        n.exp.accept(this);
        int offest = n.pos;
	    return super.visitInstVarAccess(n);
    }

    @Override
    public Object visitInstanceOf(InstanceOf n) {
        return super.visitInstanceOf(n);
    }

    @Override
    public Object visitCast(Cast n) {
        return super.visitCast(n);
    }

    @Override
    public Object visitNewObject(NewObject n) {
        return super.visitNewObject(n);
    }

    @Override
    public Object visitNewArray(NewArray n) {
        return super.visitNewArray(n);
    }

    @Override
    public Object visitCall(Call n) {
        return super.visitCall(n);
    }

    @Override
    public Object visitLocalVarDecl(LocalVarDecl n) {
        return super.visitLocalVarDecl(n);
    }

    @Override
    public Object visitCallStatement(CallStatement n) {
        return super.visitCallStatement(n);
    }

    @Override
    public Object visitBlock(Block n) {
        return super.visitBlock(n);
    }

    @Override
    public Object visitIf(If n) {
        return super.visitIf(n);
    }

    @Override
    public Object visitWhile(While n) {
        stackHeight = n.stackHeight;
        code.emit(n, "j while_enter_"+n.uniqueId);
        code.emit(n, "while_top_"+n.uniqueId+":");
        n.body.accept(this);
        code.emit(n, "while_enter_"+n.uniqueId+":");
        n.exp.accept(this);
        code.emit(n, "lw $t0,($sp)");
        code.emit(n, "addu $sp,$sp,4");
        stackHeight -= 4;
        code.emit(n, "bne $t0,$zero,while_top_"+n.uniqueId);
        code.emit(n, "break_target_"+n.uniqueId+":");
	    return super.visitWhile(n);
    }

    @Override
    public Object visitBreak(Break n) {
        return super.visitBreak(n);
    }

    @Override
    public Object visitAssign(Assign n) { //InstVarAccess
        return super.visitAssign(n);
    }

    @Override
    public Object visitLabel(Label n) {
        return super.visitLabel(n);
    }

    @Override
    public Object visitSwitch(Switch n) {
        return super.visitSwitch(n);
    }

    @Override
    public Object visitMethodDecl(MethodDecl n) {
        return super.visitMethodDecl(n);
    }

    @Override
    public Object visitMethodDeclNonVoid(MethodDeclNonVoid n) {
        return super.visitMethodDeclNonVoid(n);
    }

    @Override
    public Object visitProgram(Program n) {
        code.emit(n, " .text");
        code.emit(n, "main:"); // main label
        // for not, just emit code to exit cleanly
        code.emit(n, " li $v0,10");
        code.emit(n, " syscall");

        // For Part A of Assignment 5 (when we are not generating vtables), define dummy
        // labels that are referenced in mjLib.asm.
        // ****** the
        code.emit(n, "CLASS_String:");
        code.emit(n, "dataArrayVTableStart:");
        return null;
    }
}


	
