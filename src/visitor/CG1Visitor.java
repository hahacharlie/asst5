package visitor;

import syntaxtree.*;

import java.util.*;

import errorMsg.*;
import java.io.*;
import java.awt.Point;

//the purpose here is to annotate things with their offsets:
// - formal parameters, with respect to the (callee) frame
// - instance variables, with respect to their slot in the object
// - methods, with respect to their slot in the v-table
public class CG1Visitor extends ASTvisitor {
	
	// error message object
	private ErrorMsg errorMsg;
	
	// IO stream to which we will emit code
	private CodeStream code;
	
	// v-table offset of next method we encounter
	private int currentMethodOffset;
	
	// offset in object of next "object" instance variable we encounter
	private int currentObjInstVarOffset;
	
	// offset in object of next "data" instance variable we encounter
	private int currentDataInstVarOffset;
	
	// stack-offset of next formal parameter we encounter
	private int currentFormalVarOffset;
	
	// stack method tables for current class and all superclasses
	private Stack<ArrayList<String>> superclassMethodTables;
	
	// current method table
	private ArrayList<String> currentMethodTable;
	
	// to collect the array types that are referenced in the code
	private HashSet<ArrayType> arrayTypesInCode;
	
	private static int SUPER_VPOINTER_SIZE = 0;
	
	////************** BEGIN STARTER-FILE ALREADY-IMPLEMENETED HELPER-METHODS *************
	////
	////********** CS 358 STUDENTS SHOULD NOT MODIFY THESE UNLESS THEY
	////**********REALLY KNOW WHAT THEY ARE DOING
	
	public CG1Visitor(ErrorMsg e, PrintStream out) {
		initInstanceVars(e, out);
	}
	
	public static String vtableNameFor(Type t) {
		if (t instanceof ArrayType) {
			return "_ARRAY_" + vtableNameFor(((ArrayType)t).baseType);
		}
		else if (t instanceof IdentifierType) {
			return t.toString2();
		}
		else if (t instanceof IntegerType) {
			return "_INT";
		}
		else if (t instanceof BooleanType) {
			return "_BOOLEAN";
		}
		else if (t instanceof VoidType) {
			return "_VOID";
		}
		else if (t instanceof NullType) {
			return "_NULLTYPE";
		}
		else return "_UNKNOWNTYPE";
	}
	
	private static String printStringNameFor(Type t, String prefix, String suffix) {
		if (t instanceof ArrayType) {
			return "[" + printStringNameFor(((ArrayType)t).baseType,"L",";");
		}
		else if (t instanceof IdentifierType) {
			return printStringNameFor(((IdentifierType)t).link, prefix, suffix);
		}
		else if (t instanceof IntegerType) {
			return "I";
		}
		else if (t instanceof BooleanType) {
			return "Z";
		}
		else return "?";
	}
	
	private static String printStringNameFor(ClassDecl cd, String prefix, String suffix) {
		String name = cd.name;
		if (name.equals("String") || name.equals("Object")) {
			name = "java.lang." + name;
		}
		return prefix + name + suffix;
	}
	
	private void emitPrintStringNameFor(ClassDecl cd) {
		IdentifierType temp = new IdentifierType(cd.pos, cd.name);
		temp.link = cd;
		emitPrintStringNameFor(temp);
	}
	
	private void emitPrintStringNameFor(Type t) {
		String printString = printStringNameFor(t,"","");
		for (int i = (printString.length()+3)%4; i < 3; i++) {
			code.emit(t, " .byte 0"); // padding
		}
		
		for (int i = 0; i < printString.length(); i++) {
			char ch = printString.charAt(i);
			int val = (int)ch;
			if (i == 0) {
				code.emit(t, " .byte "+ (val | 0x80) + " # '"+ch+"' with high bit set");
			}
			else {
				code.emit(t, " .byte "+val+ " # '"+ch+"'");
			}
		}
	}
	
	private void emitArrayTypeVtables() {
		// vtable for class Object, which we will use
		currentMethodTable = superclassMethodTables.peek();
		
		// array-lists for object base types ([0]) and non-object base
		// types ([1]); this ensures that the data-array objects are
		// emitted last
		ArrayList<ArrayType>[] arrayTypes = new ArrayList[2];
		arrayTypes[0] = new ArrayList<ArrayType>();
		arrayTypes[1] = new ArrayList<ArrayType>();
		for (ArrayType at : arrayTypesInCode) {
			int idx = (at.baseType instanceof IntegerType || at.baseType instanceof BooleanType) ? 1 : 0;
			arrayTypes[idx].add(at);
		}
		for (int j = 0; j < 2; j++) {
			// emit label to separate array-of-object and array-of-data vtables
			if (j == 1) {
				code.emit(null, "dataArrayVTableStart:");
			}
			for (ArrayType at : arrayTypes[j]) {
				emitPrintStringNameFor(at);
				code.emit(at, "CLASS_"+vtableNameFor(at)+":");
				for (int i = SUPER_VPOINTER_SIZE; i < currentMethodTable.size(); i++) {
					code.emit(at, " .word "+currentMethodTable.get(i));
				}
				code.emit(at, "END_CLASS_"+vtableNameFor(at)+":");
			}
		}
	}
	
	private void initInstanceVars(ErrorMsg e, PrintStream out) {
		errorMsg = e;
		currentMethodOffset = 0;
		currentObjInstVarOffset = 0;
		currentDataInstVarOffset = 0;
		code = new CodeStream(out, e);
		superclassMethodTables = new Stack<ArrayList<String>>();
		superclassMethodTables.addElement(new ArrayList<String>());
		arrayTypesInCode = new HashSet<ArrayType>();
	}
	
	////************** END STARTER-FILE ALREADY-IMPLEMENETED HELPER-METHODS *************

	////********* THE CS 358 STUDENT'S CODE (VISITORS AND HELPER METHODS) SHOULD STARTER HERE
}
	
