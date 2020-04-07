package visitor;

import syntaxtree.*;

import java.util.*;

import errorMsg.*;
import java.io.*;
import java.awt.Point;
 
// the purpose here is to generate string literal data

public class CG2Visitor extends CG2VisitorSimple {
	
	// error message object
	private ErrorMsg errorMsg;
	
	// IO stream to which we will emit code
	private CodeStream code;

	// hash-table that keeps track of unique representative for
	// each string literal
	private Hashtable<String,StringLiteral> stringTable;
	
	public CG2Visitor(ErrorMsg e, PrintStream out) {
		super(e, out);
		initInstanceVars(e, out);
	}
	
	@Override
	public Object visitProgram(Program p) {
		code.emit(p, " .data");
		p.classDecls.accept(this);
		code.flush();
		return null;
	}
	
	private void initInstanceVars(ErrorMsg e, PrintStream out) {
		errorMsg = e;
		code = new CodeStream(out, e);
		stringTable = new Hashtable<String,StringLiteral>();	
	}
	
}

	
