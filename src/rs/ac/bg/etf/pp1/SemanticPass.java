package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.PrintStatement;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;
import rs.ac.bg.etf.pp1.ast.*;

public class SemanticPass extends VisitorAdaptor {


	int printCallCount = 0;
	int varDeclCount = 0;
	

	boolean errorDetected = false;
	
	Logger log = Logger.getLogger(getClass());


	public void report_error(String message, SyntaxNode info) {
		errorDetected = true;
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message); 
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.info(msg.toString());
	}
	
    public void visit(PrintStatement print) {
		printCallCount++;
	}
    
    public void visit(ProgName progName) {
    	progName.obj = Tab.insert(Obj.Prog, progName.getProgName(), Tab.noType);
    	Tab.openScope();
    }
    
    public void visit(Program program) {
    	
    	Tab.chainLocalSymbols(program.getProgName().obj);
    	Tab.closeScope();
    }
    
    public void visit(VarDecl varDecl) {
    	varDeclCount++;
    	Obj varNode = Tab.insert(Obj.Var, varDecl.getVarName(), varDecl.getType().struct); 
    	
    }
    
    public void visit(Type type) {
    	Obj typeNode = Tab.find(type.getTypeName());
    	if(typeNode == Tab.noObj) {
    		report_error("Nije pronadjen tip " + type.getTypeName() + " u tabeli simbola. ", null);
    		type.struct = Tab.noType;
    	}else {
    		if(Obj.Type == typeNode.getKind()) {
    			type.struct = typeNode.getType(); 
    		}
    		else {
    			report_error("Greska: Ime " + type.getTypeName() + " ne predstavlja tip!", type);
    			type.struct = Tab.noType;
    		}
    	}
    }
}
