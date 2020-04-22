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

	@Override
	public Object visitStringLiteral(StringLiteral sl) {
		if (stringTable.containsKey(sl.str)) {
			sl.uniqueCgRep = stringTable.get(sl.str);
		} else {
			stringTable.put(sl.str, sl);
			sl.uniqueCgRep = sl;
			for (i=0; i<sl.str.length()-1; i++) {
				//TODO: fill in:
				// for each character in string, emit .byte N, where N is
				// the integer code for the character (e.g., 32 for the space character)
				// up to three .byte 0 directives in order to make the
				// total number of .byte directives an even multiple of 4
			}
			code.emit(sl, ".word CLASS_String");
			int characters = sl.str.length()/4+1;
			code.emit(sl, ".word "+characters);
			code.emit(sl, ".word "+"-"+sl.str.length());
			code.emit(sl, "strLit_"+sl.uniqueId);
		}
		return null;
	}
}

	
