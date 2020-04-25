package visitor;

import syntaxtree.*;

import errorMsg.*;
import wrangLR.generator.util.ReverseArrayIterator;

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
	    code.indent(n);
        //code.emit(n, "# Before IntegerLiteral stackHeight equals: "+stackHeight);
	    code.emit(n, "subu $sp,$sp,8");
        stackHeight += 8;
        code.emit(n, "sw $s5,4($sp)");
        code.emit(n,"li $t0,"+n.val);
        code.emit(n, "sw $t0,($sp)");
        //code.emit(n, "# After IntegerLiteral stackHeight equals: "+stackHeight);
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitNull(Null n) {
        code.indent(n);
        code.emit(n,"subu $sp,$sp,4");
        stackHeight += 4;
        code.emit(n, "sw $zero,($sp)");
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitTrue(True n) {
        code.indent(n);
        code.emit(n, "subu $sp,$sp,4");
        stackHeight += 4;
        code.emit(n, "li $t0,1");
        code.emit(n, "sw $t0,($sp)");
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitFalse(False n) {
        code.indent(n);
        code.emit(n,"subu $sp,$sp,4");
        stackHeight += 4;
        code.emit(n, "sw $zero,($sp)");
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitStringLiteral(StringLiteral n) {
        code.indent(n);
        code.emit(n, "# Before StringLiteral stackHeight equals: "+stackHeight);
        code.emit(n, "subu $sp,$sp,4");
        stackHeight += 4;
        code.emit(n, "la $t0,strLit_"+n.uniqueCgRep.uniqueId);
        code.emit(n, "sw $t0,($sp)");
        code.emit(n, "# After StringLiteral stackHeight equals: "+stackHeight);
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitThis(This n) {
        code.indent(n);
        code.emit(n, "subu $sp,$sp,4");
        stackHeight += 4;
        code.emit(n, "sw $s2,($sp)");
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitSuper(Super n) {
        code.indent(n);
        code.emit(n, "subu $sp,$sp,4");
        stackHeight += 4;
        code.emit(n, "sw $s2,($sp)");
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitIdentifierExp(IdentifierExp n) {
        code.indent(n);
        //code.emit(n, "# Before IdentifierExp stackHeight equals: "+stackHeight);
        if (n.link instanceof InstVarDecl) {
            int NNN = n.link.offset;
            code.emit(n, "lw $t0,"+NNN+"($s2)");
        } else {
            int NNN = n.link.offset+stackHeight;
            code.emit(n, "lw $t0,"+NNN+"($sp)" + "# local: stackHeight:" + stackHeight + " var-offset:" + n.link.offset);
        }
        if (n.type instanceof IntegerType) {
            code.emit(n, "subu $sp,$sp,8");
            stackHeight += 8;
            code.emit(n, "sw $s5,4($sp)");
            code.emit(n, "sw $t0,($sp)");
        }else if (!(n.type instanceof VoidType)) {
            code.emit(n, "subu $sp,$sp,4");
            stackHeight += 4;
            code.emit(n, "sw $t0,($sp)");
        }
        //code.emit(n, "# After IdentifierExp stackHeight equals: "+stackHeight);
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitNot(Not n) {
        code.indent(n);
        n.exp.accept(this);
        code.emit(n, "lw $t0,($sp)");
        code.emit(n, "xor $t0,$t0,1");
        code.emit(n, "sw $t0,($sp)");
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitPlus(Plus n) {
        code.indent(n);
        code.emit(n, "# Before Plus stackHeight equals: "+stackHeight);
        n.left.accept(this);
        n.right.accept(this);
        //code.emit(n, "# Plus two sides traverse stackHeight equals: "+stackHeight);
        code.emit(n, "lw $t0,($sp)");
        code.emit(n, "lw $t1,8($sp)");
        code.emit(n, "addu $t0,$t0,$t1");
        code.emit(n, "addu $sp,$sp,8");
        stackHeight -= 8;
        code.emit(n, "sw $t0,($sp)");
        code.emit(n, "# After Plus stackHeight equals: "+stackHeight);
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitMinus(Minus n) {
        code.indent(n);
	    code.emit(n, "# Before Minus stackHeight equals: "+stackHeight);
        n.left.accept(this);
        n.right.accept(this);
        //code.emit(n, "# sMinus two sides traverse tackHeight equals: "+stackHeight);
        code.emit(n, "lw $t0,($sp)");
        code.emit(n, "lw $t1,8($sp)");
        code.emit(n, "subu $t0,$t1,$t0");
        code.emit(n, "addu $sp,$sp,8");
        stackHeight -= 8;
        code.emit(n, "sw $t0,($sp)");
        code.emit(n, "# After Minus stackHeight equals: "+stackHeight);
        code.unindent(n);
        return null;
    }

    @Override
    public Object visitTimes(Times n) {
        code.indent(n);
        n.left.accept(this);
        n.right.accept(this);
        code.emit(n, "lw $t0,($sp)");
        code.emit(n, "lw $t1,8($sp)");
        code.emit(n, "mult $t0,$t1");
        code.emit(n, "mflo $t0");
        code.emit(n, "addu $sp,$sp,8");
        stackHeight -= 8;
        code.emit(n, "sw $t0,($sp)");
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitDivide(Divide n) {
        code.indent(n);
        n.left.accept(this);
        n.right.accept(this);
        code.emit(n, "jal divide");
        stackHeight += 8;
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitRemainder(Remainder n) {
        code.indent(n);
        n.left.accept(this);
        n.right.accept(this);
        code.emit(n, "jal remainder");
        stackHeight += 8;
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitEquals(Equals n) {
        code.indent(n);
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
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitGreaterThan(GreaterThan n) {
        code.indent(n);
        n.left.accept(this);
        n.right.accept(this);
        code.emit(n, "lw $t0,($sp)");
        code.emit(n, "lw $t1,8($sp)");
        code.emit(n, "sgt $t0,$t1,$t0");
        code.emit(n, "addu $sp,$sp,12");
        stackHeight -= 12;
        code.emit(n, "sw $t0,($sp)");
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitLessThan(LessThan n) {
        code.indent(n);
        n.left.accept(this);
        n.right.accept(this);
        code.emit(n, "lw $t0,($sp)");
        code.emit(n, "lw $t1,8($sp)");
        code.emit(n, "slt $t0,$t1,$t0");
        code.emit(n, "addu $sp,$sp,12");
        stackHeight -= 12;
        code.emit(n, "sw $t0,($sp)");
        code.unindent(n);
        return null;
    }

    @Override
    public Object visitAnd(And n) {
        code.indent(n);
        n.left.accept(this);
        code.emit(n, "lw $t0,($sp)");
        code.emit(n, "beq $t0,$zero,skip_"+n.uniqueId);
        code.emit(n, "addu $sp,$sp,4");
        stackHeight -= 4;
        n.right.accept(this);
        code.emit(n, "skip_"+n.uniqueId+":");
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitOr(Or n) { // TODO: Further Inspection needed.
        code.indent(n);
        n.left.accept(this);
        code.emit(n, "lw $t0,($sp)");
        code.emit(n, "bne $t0,$zero,skip_"+n.uniqueId);
        code.emit(n, "addu $sp,$sp,4");
        stackHeight -= 4;
        n.right.accept(this);
        code.emit(n, "skip_"+n.uniqueId+":");
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitArrayLength(ArrayLength n) {
        code.indent(n);
        n.exp.accept(this);
        code.emit(n, "lw $t0, ($sp)");
        code.emit(n, "beq $t0,$zero,nullPtrException");
        code.emit(n, "lw $t0,-4($t0)");
        code.emit(n, "sw $s5,($sp)");
        code.emit(n, "subu $sp,4");
        stackHeight += 4;
        code.emit(n, "sw $t0,($sp)");
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitArrayLookup(ArrayLookup n) {
        code.indent(n);
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
        code.unindent(n);
        return super.visitArrayLookup(n);
    }

    @Override
    public Object visitInstVarAccess(InstVarAccess n) {
        code.indent(n);
        n.exp.accept(this);
        int offset = n.varDec.offset;
        code.emit(n, "lw $t0,($sp)");
        code.emit(n, "beq $t0,$zero,nullPtrException");
        code.emit(n, "lw $t0,"+offset+"($t0)");
        if (n.type instanceof IntegerType) {
            code.emit(n, "subu $sp,$sp,4");
            stackHeight += 4;
            code.emit(n, "sw $s5,4($sp)");
            code.emit(n, "sw $t0,($sp)");
        } else {
            code.emit(n, "sw $t0,($sp)");
        }
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitInstanceOf(InstanceOf n) {
        code.indent(n);
        n.exp.accept(this);
        code.emit(n, "la $t0,CLASS_"+CG1Visitor.vtableNameFor(n.type));
        code.emit(n, "la $t1,END_CLASS_"+CG1Visitor.vtableNameFor(n.type));
        code.emit(n, "jal instanceOf");
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitCast(Cast n) {
        code.indent(n);
        n.exp.accept(this);
        if (n.type.getClass().equals(n.castType.getClass().getGenericSuperclass())) {
            code.emit(n, "la $t0,CLASS_"+CG1Visitor.vtableNameFor(n.type));
            code.emit(n, "la $t1,END_CLASS_"+CG1Visitor.vtableNameFor(n.type));
            code.emit(n, "jal checkCast");
        }
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitNewObject(NewObject n) {
        code.indent(n);
        code.emit(n, "# Before NewObject stackHeight equals: "+stackHeight);
        int numOfObjInstVar = n.objType.link.numObjInstVars;
        int numOfDataInstVar = n.objType.link.numDataInstVars+1;
        code.emit(n, " li $s6,"+numOfDataInstVar);
        code.emit(n, " li $s7,"+numOfObjInstVar);
        code.emit(n, " jal newObject");
        if (n.type instanceof IntegerType) {
            stackHeight -= 8;
        } else {
            stackHeight -= 4;
        }
        code.emit(n, " la $t0,CLASS_"+n.objType.link.name);
        code.emit(n, " sw $t0,-12($s7)");
        code.emit(n, "# After NewObject stackHeight equals: "+stackHeight);
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitNewArray(NewArray n) {
        code.indent(n);
        n.sizeExp.accept(this);
        code.emit(n, "lw $s7,($sp)");
        code.emit(n, "addu $sp,$sp,8");
        stackHeight -= 8;
        code.emit(n, "li $s6,1");
        code.emit(n, "jal newObject");
        stackHeight -= 4;
        code.emit(n, "la $t0,CLASS_"+CG1Visitor.vtableNameFor(n.type));
        code.emit(n, "sw $t0,-12($s7)");
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitCall(Call n) {
        code.indent(n);
        //code.emit(n, "# Before call stackHeight equals: "+stackHeight);
	    if (n.obj instanceof Super) {
            int oldStackHeight = stackHeight;
            n.obj.accept(this);
            n.parms.accept(this);
            if (n.methodLink.pos < 0) {
                code.emit(n, " jal "+n.methodLink.name+"_"+n.methodLink.classDecl.name);
            } else {
                code.emit(n, " jal fcn_"+n.methodLink.uniqueId+"_"+n.methodLink.name);
            }
            if (n.type instanceof IntegerType) {
                stackHeight = oldStackHeight + 8;
            } else if (n.type instanceof VoidType) {
                stackHeight = oldStackHeight;
            } else {
                stackHeight = oldStackHeight + 4;
            }
        } else {
            int oldStackHeight = stackHeight;
            n.obj.accept(this);
//            if (n.obj.toString().equals("this")) {
//                code.emit(n, " lw $zero,($sp)");
//            }
            n.parms.accept(this);
            int MMM = n.methodLink.thisPtrOffset-4;
            int NNN = 4*n.methodLink.vtableOffset;
            code.emit(n, "# Now the vtableOffset is:" + n.methodLink.vtableOffset);
            code.emit(n, " lw $t0,"+MMM+"($sp)");
            code.emit(n, " beq $t0,$zero,nullPtrException");
            code.emit(n, " lw $t0,-12($t0)");
            code.emit(n, " lw $t0,"+NNN+"($t0)");
            code.emit(n, " jalr $t0");
            code.emit(n, "# stackHeight before change in call is " + stackHeight);
            if (n.obj.type instanceof IntegerType) {
                stackHeight = oldStackHeight + 8;
            } else if (n.obj.type instanceof VoidType) {
                stackHeight = oldStackHeight;
            } else {
                stackHeight = oldStackHeight + 4;
            }
            code.emit(n, "# stackHeight after change in call is " + stackHeight);
        }
        //code.emit(n, "# After Call stackHeight equals: "+stackHeight);
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitLocalVarDecl(LocalVarDecl n) {
        code.indent(n);
        n.initExp.accept(this);
        n.offset = -stackHeight;
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitCallStatement(CallStatement n) {
        code.indent(n);
        //code.emit(n, "# Before CallStatement stackHeight equals: "+stackHeight);
        n.callExp.accept(this);
        if (n.callExp.type instanceof IntegerType) {
            code.emit(n, "addu $sp,$sp,8");
            stackHeight -= 8;
        } else if (!(n.callExp.type instanceof VoidType)) {
            code.emit(n, "addu $sp,$sp,4");
            stackHeight -= 4;
        }
        //code.emit(n, "# After CallStatement stackHeight equals: "+stackHeight);
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitBlock(Block n) {
        code.indent(n);
        int oldStackHeight = stackHeight;
        n.stmts.accept(this);
        if (stackHeight != oldStackHeight) {
            int DDD = stackHeight - oldStackHeight;
            code.emit(n, "addu $sp,"+DDD);
        }
        stackHeight = oldStackHeight;
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitIf(If n) {
        code.indent(n);
        n.exp.accept(this);
        code.emit(n, "lw $t0,($sp)");
        code.emit(n, "addu $sp,$sp,4");
        stackHeight -= 4;
        code.emit(n, "beq $t0,$zero,if_else_"+n.uniqueId);
        n.trueStmt.accept(this);
        code.emit(n, "j if_done_"+n.uniqueId);
        code.emit(n, "if_else_"+n.uniqueId+":");
        n.falseStmt.accept(this);
        code.emit(n, "if_done_"+n.uniqueId+":");
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitWhile(While n) {
        code.indent(n);
        n.stackHeight = stackHeight;
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
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitBreak(Break n) {
        code.indent(n);
        int diff = stackHeight - n.breakLink.stackHeight;
        if (diff != 0) {
            code.emit(n, "addu $sp,"+diff);
        }
        code.emit(n, "j break_target_"+n.uniqueId);
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitAssign(Assign n) {
        code.indent(n);
        if (n.lhs instanceof IdentifierExp) {
            n.rhs.accept(this);
            code.emit(n, "lw $t0,($sp)");
            if (((IdentifierExp) n.lhs).link instanceof InstVarDecl) {
                int NNN = ((IdentifierExp) n.lhs).link.offset;
                code.emit(n, "sw $t0,"+NNN+"($s2)");
            } else {
                int MMM = ((IdentifierExp) n.lhs).link.offset + stackHeight;
                code.emit(n, "sw $t0,"+MMM+"($sp)");
            }
            if (n.lhs.type instanceof IntegerType) {
                code.emit(n, "addu $sp,$sp,8");
                stackHeight -= 8;
            } else {
                code.emit(n, "addu $sp,$sp,4");
                stackHeight -= 4;
            }
        } else if (n.lhs instanceof InstVarAccess) {
            ((InstVarAccess) n.lhs).exp.accept(this);
            n.rhs.accept(this);
            code.emit(n, "lw $t0,($sp)");
            if (n.rhs.type instanceof IntegerType) {
                code.emit(n, "lw $t1,8($sp)");
            } else {
                code.emit(n, "lw $t1,4($sp)");
            }
            code.emit(n, "beq $t1,$zero,nullPtrException");
            int NNN = ((InstVarAccess) n.lhs).varDec.offset;
            code.emit(n, "sw $t0,"+NNN+"($t1)");
            if (n.lhs.type instanceof IntegerType) {
                code.emit(n, "addu $sp,$sp,12");
                stackHeight -= 12;
            } else {
                code.emit(n, "addu $sp,$sp,8");
                stackHeight -= 8;
            }
        } else if (n.lhs instanceof ArrayLookup) {
            ((ArrayLookup) n.lhs).arrExp.accept(this);
            ((ArrayLookup) n.lhs).idxExp.accept(this);
            n.rhs.accept(this);
            code.emit(n, "lw $t0,($sp)");
            if (n.rhs.type instanceof IntegerType) {
                code.emit(n, "lw $t1,16($sp)");
            } else {
                code.emit(n, "lw $t1,12($sp)");
            }
            code.emit(n, "beq $t1,$zero,nullPtrException");
            if (n.rhs.type instanceof IntegerType) {
                code.emit(n, "lw $t2,8($sp)");
            } else {
                code.emit(n, "lw $t2,4($sp)");
            }
            code.emit(n, "lw $t3,-4($t1)");
            code.emit(n, "bgeu $t2,$t3,arrayIndexOutOfBounds");
            code.emit(n, "sll $t2,$t2,2");
            code.emit(n, "addu $t2,$t2,$t1");
            code.emit(n, "sw $t0,($t2)");
            if (n.rhs.type instanceof IntegerType) {
                code.emit(n, "addu $sp,$sp,20");
                stackHeight -= 20;
            } else {
                code.emit(n, "addu $sp,$sp,16");
                stackHeight -= 16;
            }
        }
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitLabel(Label n) {
        code.indent(n);
        stackHeight = n.enclosingSwitch.stackHeight;
        code.emit(n, "case_label_"+n.uniqueId);
        code.unindent(n);
	    return null;
    }

    @Override
    public Object visitMethodDeclVoid(MethodDeclVoid n) {
        code.indent(n);
        //code.emit(n, "# Before MethodDeclVoid the stackHeight is " + stackHeight);
        code.emit(n, ".globl fcn_"+n.uniqueId+"_"+n.name);
        code.emit(n, "fcn_"+n.uniqueId+"_"+n.name+":");
        code.emit(n, " subu $sp,$sp,4");
        stackHeight += 4;
        code.emit(n, " sw $s2,($sp)");
        int NNN = n.thisPtrOffset;
        code.emit(n, " lw $s2,"+NNN+"($sp)");
        code.emit(n, " sw $ra,"+NNN+"($sp)");
        stackHeight = 0;
        n.stmts.accept(this); // generate code for the method's body
        code.emit(n, "# the stackHeight is " + stackHeight);
        int PPP = stackHeight+n.thisPtrOffset;
        int QQQ = stackHeight;
        code.emit(n, " lw $ra,"+PPP+"($sp)");
        code.emit(n, " lw $s2,"+QQQ+"($sp)");
        int RRR = stackHeight + n.thisPtrOffset + 4;
        code.emit(n, " addu $sp,$sp,"+RRR);
        code.emit(n, " jr $ra");
        //code.emit(n, "# After MethodDeclVoid the stackHeight is " + stackHeight);
        code.unindent(n);
        return null;
    }

    @Override
    public Object visitMethodDeclNonVoid(MethodDeclNonVoid n) {
        code.indent(n);
        code.emit(n, "# Before MethodDeclNonVoid the stackHeight is " + stackHeight);
        code.emit(n, ".globl fcn_"+n.uniqueId+"_"+n.name);
        code.emit(n, "fcn_"+n.uniqueId+"_"+n.name+":");
        code.emit(n, "subu $sp,$sp,4");
        stackHeight += 4;
        code.emit(n, "sw $s2,($sp)");
        int NNN = n.thisPtrOffset;
        code.emit(n, "lw $s2,"+NNN+"($sp)");
        code.emit(n, "sw $ra,"+NNN+"($sp)");
        stackHeight = 0;
        n.stmts.accept(this);
        n.rtnExp.accept(this);
        int PPP = stackHeight+n.thisPtrOffset;
        int QQQ = stackHeight;
        code.emit(n, "lw $ra,"+PPP+"($sp)");
        code.emit(n, "lw $s2,"+QQQ+"($sp)");
        code.emit(n, "lw $t0,($sp)");
        if (n.rtnType instanceof IntegerType) {
            int SSS = stackHeight+n.thisPtrOffset-4;
            int TTT = stackHeight+n.thisPtrOffset;
            code.emit(n, "sw $t0,"+SSS+"($sp)");
            code.emit(n, "sw $s5,"+TTT+"($sp)");
        } else {
            int SSS = stackHeight+n.thisPtrOffset;
            code.emit(n, "sw $t0,"+SSS+"($sp)");
        }
        int RRR = stackHeight + n.thisPtrOffset + 4;
        if (n.rtnType instanceof IntegerType) {
            RRR -= 8;
        } else {
            RRR -= 4;
        }
        code.emit(n, "addu $sp,$sp,"+ RRR);
        code.emit(n, "jr $ra");
        code.emit(n, "# After MethodDeclNonVoid the stackHeight is " + stackHeight);
        code.unindent(n);
        return null;
    }

    @Override
    public Object visitProgram(Program n) {
        code.indent(n);
        code.emit(n, " .text");
        code.emit(n, " .globl main");
        code.emit(n, "main:");
        //code.emit(n, "# initialize registers, etc.");
        code.emit(n, "jal vm_init");
        stackHeight = 0;
        n.mainStatement.accept(this);
        code.emit(n, "# exit program");
        code.emit(n, " li $v0,10");
        code.emit(n, " syscall");
        //code.emit(n, "# program exited...............");
        n.classDecls.accept(this);
        code.flush();
        //code.emit(n, "# After Program stackHeight equals: "+stackHeight);
        code.unindent(n);
        return null;
    }
}


	
