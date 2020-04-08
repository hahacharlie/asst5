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
        return super.visitIntegerLiteral(n);
    }

    @Override
    public Object visitNull(Null n) {
        return super.visitNull(n);
    }

    @Override
    public Object visitTrue(True n) {
        return super.visitTrue(n);
    }

    @Override
    public Object visitFalse(False n) {
        return super.visitFalse(n);
    }

    @Override
    public Object visitStringLiteral(StringLiteral n) {
        return super.visitStringLiteral(n);
    }

    @Override
    public Object visitThis(This n) {
        return super.visitThis(n);
    }

    @Override
    public Object visitSuper(Super n) {
        return super.visitSuper(n);
    }

    @Override
    public Object visitIdentifierExp(IdentifierExp n) {
        return super.visitIdentifierExp(n);
    }

    @Override
    public Object visitNot(Not n) {
        return super.visitNot(n);
    }

    @Override
    public Object visitPlus(Plus n) {
        visitPlus((Plus)n.left); //traverse left-expression
        visitPlus((Plus)n.right); //traverse right-expression
        //emit code
        code.emit(n, "lw $t0, ($sp)");
        code.emit(n, "lw $t1, 8($sp)");
        code.emit(n, "addu $t0, $t0, $t1");
        code.emit(n, "addu $sp, $sp, 8");
        code.emit(n, "sw $t0, ($sp)");
        stackHeight = stackHeight - 8; //subtract 8 from stackHeight
	    return null;
    }

    @Override
    public Object visitMinus(Minus n) {
        visitMinus((Minus)n.left); //traverse left-expression
        visitMinus((Minus)n.right); //traverse right-expression
        //emit code
        code.emit(n, "lw $t0, ($sp)");
        code.emit(n, "lw $t1, 8($sp)");
        code.emit(n, "subu $t0, $t0, $t1");
        code.emit(n, "subu $sp, $sp, 8");
        code.emit(n, "sw $t0, ($sp)");
        stackHeight = stackHeight - 8; //subtract 8 from stackHeight
        return null;
    }

    @Override
    public Object visitTimes(Times n) {
        return super.visitTimes(n);
    }

    @Override
    public Object visitDivide(Divide n) {
        return super.visitDivide(n);
    }

    @Override
    public Object visitEquals(Equals n) {
        return super.visitEquals(n);
    }

    @Override
    public Object visitGreaterThan(GreaterThan n) {
        return super.visitGreaterThan(n);
    }

    @Override
    public Object visitAnd(And n) {
        return super.visitAnd(n);
    }

    @Override
    public Object visitArrayLength(ArrayLength n) {
        return super.visitArrayLength(n);
    }

    @Override
    public Object visitArrayLookup(ArrayLookup n) {
        return super.visitArrayLookup(n);
    }

    @Override
    public Object visitInstVarAccess(InstVarAccess n) {
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
        return super.visitWhile(n);
    }

    @Override
    public Object visitBreak(Break n) {
        return super.visitBreak(n);
    }

    @Override
    public Object visitAssign(Assign n) {
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


	
