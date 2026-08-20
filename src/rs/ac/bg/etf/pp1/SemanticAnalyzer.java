package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.Stack;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticAnalyzer extends VisitorAdaptor {

	int printCallCount = 0;
	int varDeclCount = 0;
	int constDeclCount = 0;
	int arrDeclCount = 0;
	int localVariablesMain = 0;
	int methodsInProgram = 0;
	int statementsInMain = 0;
	int functionCallsInMain = 0;
	
	
	boolean errorDetected = false;
	int nVars;
	
	Type currType;// Trenutni tip za constDecl i varDecl
	
	Obj currentMethodDecl = null; // Trenutna metoda koja se obradjuje
    private TMethodDecl mDecl;

    Stack<Obj> methodCallStackObj = new Stack<>();// Objekat pozvane metode za svaki nivo poziva
	Stack<Integer> methodCallStackCounter = new Stack<>();
	
	int formalParsCount = 0; // Broj formalnih parametara za opis i pretragu metode
	
	boolean returnFound = false; // Da li je pronadjen return u telu metode

	boolean mainFound = false;

	Stack<Integer> forLoopCallStack = new Stack<>(); // Da li su trenutni neterminali u ovkiru for petlje
	
	Logger log = Logger.getLogger(getClass());
	
	
	//pomocne funkcije
	
	//Dohvata strukturu niza za odredjen tip
	public Struct getArrayStruct(Struct baseType) {
		switch(baseType.getKind()) {
		case Struct.Int:{return TabExtended.intArrayType;}
		case Struct.Bool:{return TabExtended.boolArrayType;}
		case Struct.Char:{return TabExtended.charArrayType;}
		default: return TabExtended.noType;
		}
	}
	
	//Provera da li je promenljiva vec deklarisana u istom opsegu.
	private boolean checkCurrentScope(String name) {
		Obj elem = TabExtended.currentScope.findSymbol(name);
		return elem != null;
	}
	
	public boolean checkGlobalScope(Obj obj) {
		return obj != TabExtended.noObj && !checkCurrentScope(obj.getName());
	}
	
	public void report_error(String message, SyntaxNode info) {
		errorDetected = true;
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		msg.append(".");
		log.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info, Obj sym) {
		StringBuilder msg = new StringBuilder(message); 
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		msg.append(". ");
		
		
		if(sym != null) {
			msg.append("Sym: |");
			switch (sym.getKind()) {
			case Obj.Con:   
				msg.append("Con "); 
				break;
			case Obj.Var:  
				msg.append("Var "); 
				break;
			case Obj.Type: 
				msg.append("Type "); 
				break;
			case Obj.Meth: 
				msg.append("Meth "); 
				break;
			case Obj.Fld:  
				msg.append("Fld "); 
				break;
			case Obj.Prog: 
				msg.append("Prog "); 
				break;
			}
			
			msg.append(sym.getName()).append(": ");
			
			switch (sym.getType().getKind()) {
				case Struct.None:
					msg.append("notype");
					break;
				case Struct.Int:
					msg.append("int");
					break;
				case Struct.Char:
					msg.append("char");
					break;
				case Struct.Bool:
					msg.append("bool");
					break;
				
				case Struct.Array:
					msg.append("Arr of ");
					
					switch (sym.getType().getElemType().getKind()) {
					case Struct.None:
						msg.append("notype");
						break;
					case Struct.Int:
						msg.append("int");
						break;
					case Struct.Char:
						msg.append("char");
						break;
					case Struct.Bool:
						msg.append("bool");
						break;
					case Struct.Class:
						msg.append("Class");
						break;
					}
					break;
			}
			
			msg.append(", ");
			msg.append(sym.getAdr());
			msg.append(", ");
			msg.append(sym.getLevel());
					
	
			msg.append("|");

			log.info(msg.toString());
		}
		
	}
	
	
	//Program obrada
	
	public void visit(ProgName pName) {
		pName.obj = TabExtended.insert(Obj.Prog, pName.getName(), TabExtended.noType);
		TabExtended.openScope();
		report_info("Zapoceta je obrada programa: " + pName.getName() + ". Info", pName, pName.obj);
	}
	
	public void visit(Program prog) {
		Obj obj = prog.getProgName().obj;
		TabExtended.chainLocalSymbols(obj);
		TabExtended.closeScope();

		if(!mainFound) {
			report_error("Program nema definisanu main() metodu. Greska", prog);
		}

		report_info("Zavrsena je obrada programa: " + prog.getProgName().getName() + ". Info", prog, null);
	}
	
	//Type obrada
	
	public void visit(Type type){
    	Obj typeNode = TabExtended.find(type.getTypeName());
    	if(typeNode == TabExtended.noObj){
    		report_error("Nije pronadjen tip " + type.getTypeName() + " u tabeli simbola! Greska", null);
    		type.struct = TabExtended.noType;
    	}else{
    		report_info("Pretraga prilikom obrade tipa. Info", type, typeNode);
    		if(Obj.Type == typeNode.getKind()){
    			type.struct = typeNode.getType();
    		}else{
    			report_error("Ime " + type.getTypeName() + " ne predstavlja tip! Greska", type);
    			type.struct = TabExtended.noType;
    		}
    	}
    }
	
	//VarDecl obrada
	
	public void visit (VarDeclType vDeclType) {
		currType = vDeclType.getType();
	}
	
	public void visit (VarDecl vDecl) {
		currType = null;
	}
    
	public void visit (NoBrackVarDeclElem vDeclElem) {
		if(!checkCurrentScope(vDeclElem.getName())) {//Simbol nije deklarisan u trenutnom opsegu
			Obj obj = TabExtended.insert(Obj.Var, vDeclElem.getName(), currType.struct);
			report_info("Deklarisana promenljiva: " + vDeclElem.getName() + ". Info", vDeclElem, obj);
		}
		else {//Simbol je vec deklarisan u trenutnom opsegu
			report_error("Promenljiva " + vDeclElem.getName() + " je vec deklarisana. Greska", vDeclElem);
		}
		
	}

	public void visit (NoBrackLastVarDeclElem vDeclElem) {
		if(!checkCurrentScope(vDeclElem.getName())) {//Simbol nije deklarisan u trenutnom opsegu
			Obj obj = TabExtended.insert(Obj.Var, vDeclElem.getName(), currType.struct);
			report_info("Deklarisana promenljiva: " + vDeclElem.getName() + ". Info", vDeclElem, obj);
		}
		else {//Simbol je vec deklarisan u trenutnom opsegu
			report_error("Promenljiva " + vDeclElem.getName() + " je vec deklarisana. Greska", vDeclElem);
		}
	}
	
	public void visit (BrackVarDeclElem vDeclElem) {
		
		if(!checkCurrentScope(vDeclElem.getName())) {//Simbol nije deklarisan u trenutnom opsegu
			Struct arrayType = getArrayStruct(currType.struct);
			if(arrayType == Tab.noType) {//Ne postoji struktura niza za nas tip, verovatno nepotrebna provera
				report_error("Nije pronadjen tip niza " + currType.getTypeName() + " u tabeli simbola! Greska", null);
				
			}
			else {//Postoji struktura niza za nas tip
				Obj obj = TabExtended.insert(Obj.Var, vDeclElem.getName(), arrayType);
				report_info("Deklarisana promenljiva: " + vDeclElem.getName() + ". Info", vDeclElem, obj);
			}
		}
		else {//Simbol je vec deklarisan u trenutnom opsegu
			report_error("Promenljiva " + vDeclElem.getName() + " je vec deklarisana. Greska", vDeclElem);
		}
	}
	
	public void visit (BrackLastVarDeclElem vDeclElem) {
		
		if(!checkCurrentScope(vDeclElem.getName())) {//Simbol nije deklarisan u trenutnom opsegu
			Struct arrayType = getArrayStruct(currType.struct);
			if(arrayType == Tab.noType) {//Ne postoji struktura niza za nas tip, verovatno nepotrebna provera
				report_error("Nije pronadjen tip niza " + currType.getTypeName() + " u tabeli simbola! Greska", null);
				
			}
			else {//Postoji struktura niza za nas tip
				Obj obj = TabExtended.insert(Obj.Var, vDeclElem.getName(), arrayType);
				report_info("Deklarisana promenljiva: " + vDeclElem.getName() + ". Info", vDeclElem, obj);
			}
		}
		else {//Simbol je vec deklarisan u trenutnom opsegu
			report_error("Promenljiva " + vDeclElem.getName() + " je vec deklarisana. Greska", vDeclElem);
		}
	}
	
	//ConstDecl obrada
	
	public void visit (ConstDeclType cDeclType) {
		currType = cDeclType.getType();
	}
	
	public void visit(ConstDecl cDecl) {
		currType = null;
	}
	
	public void visit(NumConstDeclElem cDecl) {
		if(TabExtended.noObj == TabExtended.find(cDecl.getName())) { //Konstanta nije prethodno deklarisana
			
			if(currType.getTypeName().equals("int")) {//Ako je trenutni tip int dodaj u tabelu simbola
				
				Obj constant = TabExtended.insert(Obj.Con, cDecl.getName(), TabExtended.intType);
				constant.setAdr(cDecl.getNumConst().getVal());
				report_info("Deklarisana konstanta: " + cDecl.getName() +" sa vrednoscu " + cDecl.getNumConst().getVal() + ". Info", cDecl, constant);
			}
			else {//Ako trenutni tip nije int greska
				report_error("Tip konstante " + currType.getTypeName() + " i int se ne poklapaju. Greska", cDecl);
			}
			
		}
		else {// Konstanta je vec deklarisana
			report_error("Konstanta " + cDecl.getName() + " je vec deklarisana. Greska", cDecl);
		}
		
	}
	
	public void visit(CharConstDeclElem cDecl) {
		
		if(TabExtended.noObj == TabExtended.find(cDecl.getName())) { //Konstanta nije prethodno deklarisana
			
			if(currType.getTypeName().equals("char")) {//Ako je trenutni tip char dodaj u tabelu simbola
				Obj constant = TabExtended.insert(Obj.Con, cDecl.getName(), TabExtended.charType);
				constant.setAdr(cDecl.getCharConst().getVal());
				report_info("Deklarisana konstanta: " + cDecl.getName() +" sa vrednoscu " + cDecl.getCharConst().getVal() + ". Info", cDecl, constant);
			}
			
			else {//Ako trenutni tip nije char greska
				report_error("Tip konstante " + currType.getTypeName() + " i char se ne poklapaju. Greska", cDecl);
			}
			
		}
		else {// Konstanta je vec deklarisana
			report_error("Konstanta " + cDecl.getName() + " je vec deklarisana. Greska", cDecl);
		}
	}
	
	public void visit(BoolConstDeclElem cDecl) {
		
		if(TabExtended.noObj == TabExtended.find(cDecl.getName())) { //Konstanta nije prethodno deklarisana
			
			if(currType.getTypeName().equals("bool")) {//Ako je trenutni tip bool dodaj u tabelu simbola
				
				Obj constant = TabExtended.insert(Obj.Con, cDecl.getName(), TabExtended.boolType);
				if(cDecl.getBoolConst() instanceof TrueBoolConst) {
					constant.setAdr(1);
					report_info("Deklarisana konstanta: " + cDecl.getName() + " sa vrednoscu True. Info", cDecl, constant);
				}
				else {
					constant.setAdr(0);
					report_info("Deklarisana konstanta: " + cDecl.getName() + " sa vrednoscu False. Info", cDecl, constant);
				}
				
			}
			else {//Ako trenutni tip nije int greska
				report_error("Tip konstante " + currType.getTypeName() + " i char se ne poklapaju. Greska", cDecl);
			}
			
		}
		else {// Konstanta je vec deklarisana
			report_error("Konstanta " + cDecl.getName() + " je vec deklarisana. Greska", cDecl);
		}
	}
	
	//MethodDecl obrada
	
	//TODO: dodaj test za ovo!
	
	//DesignatorStatement ::= Designator LPAREN ActPars RPAREN
	//chr(e); e mora biti izraz tipa int.
	//ord(c); c mora biti tipa char.
	//len(a); a mora biti niz ili znakovni niz
	
	public void visit(TMethodDecl mDecl) {
    	currentMethodDecl = Tab.insert(Obj.Meth, mDecl.getName(), mDecl.getType().struct);
        this.mDecl = mDecl;
        mDecl.obj = currentMethodDecl;
    	formalParsCount = 0;
    	Tab.openScope();
		report_info("Obradjuje se funkcija " + mDecl.getName() + ". Info", mDecl, mDecl.obj);

		if(mDecl.getName().equals("main")) {
			mainFound = true;
		}
	}
	
	public void visit(VMethodDecl mDecl) {
    	currentMethodDecl = Tab.insert(Obj.Meth, mDecl.getName(), TabExtended.noType);
    	mDecl.obj = currentMethodDecl;
    	formalParsCount = 0;
    	Tab.openScope();
		report_info("Obradjuje se funkcija " + mDecl.getName() + ". Info", mDecl, mDecl.obj);

		if(mDecl.getName().equals("main")) {
			mainFound = true;
		}
	}
	
	public void visit(MethodDecl mDecl) {

		if(currentMethodDecl.getName().equals("main") && currentMethodDecl.getType() != TabExtended.noType) {
			report_error("Metoda main mora biti void tipa. Greska", mDecl);
		}

		if(currentMethodDecl.getName().equals("main") && formalParsCount > 0) {
			if(formalParsCount == 1) {//Cisto zbog zapisa
				report_error("Metoda main ima 1 formalni parametar sto je vece od 0! Greska", mDecl);
			}
			else {
				report_error("Metoda main ima " + formalParsCount + " formalna parametara sto je vece od 0! Greska", mDecl);
			}
		}

		if(!returnFound && currentMethodDecl.getType() != Tab.noType){//TODO proveri u izrazima jel postoji return!!!
			report_error("Funkcija " + currentMethodDecl.getName() + " nema return iskaz! Greska", mDecl);
    	}

		currentMethodDecl.setLevel(formalParsCount);
    	Tab.chainLocalSymbols(currentMethodDecl);
    	Tab.closeScope();
    	
    	returnFound = false;
    	currentMethodDecl = null;
	}
	
	//FromPars obrada
	
	public void visit(NoBrackFormParsElem fPars) {
		if(!checkCurrentScope(fPars.getName())) {// Simbol nije deklarisan u trenutnom opsegu
			Obj formPar = TabExtended.insert(Obj.Var, fPars.getName(), fPars.getType().struct);
			report_info("Deklarisan formalni parametar: " + fPars.getName() + ". Info", fPars, formPar);
			formPar.setAdr(formalParsCount);
			formalParsCount++;
		}
		else {// Simbol je vec deklarisan u trenutnom opsegu
			report_error("Formalni parametar " + fPars.getName() + " je vec deklarisan. Greska", fPars);
		}
	}
	
	public void visit(NoBrackLastFormParsElem fPars) {
		if(!checkCurrentScope(fPars.getName())) {// Simbol nije deklarisan u trenutnom opsegu
			Obj formPar = TabExtended.insert(Obj.Var, fPars.getName(), fPars.getType().struct);
			report_info("Deklarisan formalni parametar: " + fPars.getName() + ". Info", fPars, formPar);
			formPar.setAdr(formalParsCount);
			formalParsCount++;
		}
		else {// Simbol je vec deklarisan u trenutnom opsegu
			report_error("Formalni parametar " + fPars.getName() + " je vec deklarisan. Greska", fPars);
		}
	}
	
	public void visit(BrackFormParsElem fPars) {
		if(!checkCurrentScope(fPars.getName())) {// Simbol nije deklarisan u trenutnom opsegu
			Struct arrayType = getArrayStruct(fPars.getType().struct);
			if(arrayType == Tab.noType) {// Ne postoji struktura niza za nas tip, verovatno nepotrebna provera
				report_error("Nije pronadjen tip niza " + fPars.getType().getTypeName() + " u tabeli simbola! Greska", null);
				
			}
			else {// Postoji struktura niza za nas tip
				Obj formPar = TabExtended.insert(Obj.Var, fPars.getName(), arrayType);
				report_info("Deklarisana formalni parametar: " + fPars.getName() + ". Info", fPars, formPar);
				formPar.setAdr(formalParsCount);
				formalParsCount++;
			}
		}
		else {// Simbol je vec deklarisan u trenutnom opsegu
			report_error("Formalni parametar " + fPars.getName() + " je vec deklarisan. Greska", fPars);
		}
	}
	public void visit(BrackLastFormParsElem fPars) {
		if(!checkCurrentScope(fPars.getName())) {// Simbol nije deklarisan u trenutnom opsegu
			Struct arrayType = getArrayStruct(fPars.getType().struct);
			if(arrayType == Tab.noType) {// Ne postoji struktura niza za nas tip, verovatno nepotrebna provera
				report_error("Nije pronadjen tip niza " + fPars.getType().getTypeName() + " u tabeli simbola! Greska", null);
				
			}
			else {// Postoji struktura niza za nas tip
				Obj formPar = TabExtended.insert(Obj.Var, fPars.getName(), arrayType);
				report_info("Deklarisana formalni parametar: " + fPars.getName() + ". Info", fPars, formPar);
				formPar.setAdr(formalParsCount);
				formalParsCount++;
			}
		}
		else {// Simbol je vec deklarisan u trenutnom opsegu
			report_error("Formalni parametar " + fPars.getName() + " je vec deklarisan. Greska", fPars);
		}
	}
	
	
	
	
	//Statement obrada
	
	public void visit(BreakStatement statement) {
		if(forLoopCallStack.empty()) {
			report_error("Break nije u okviru for petlje. Greska", statement);
		}
	}
	
	public void visit(ContinueStatement statement) {
		if(forLoopCallStack.empty()) {
			report_error("Continue nije u okviru for petlje. Greska", statement);
		}
	}
	
	public void visit(ReturnStatement statement) {
		if(currentMethodDecl == null) {
			report_error("Return iskaz ne sme postojati izvan tela funkcije.", statement);
		}
		else {
			returnFound = true;
			if(currentMethodDecl.getType() != TabExtended.noType) {
				report_error("Funkcija " + currentMethodDecl.getName() + " mora imati povratnu vrednost ali ona nije prosledjena return naredbom. Greska", statement);
			}
		}
	}
	
	public void visit(ReturnValueStatement statement) {
		if(currentMethodDecl == null) {
			report_error("Return iskaz ne sme postojati izvan tela funkcije.", statement);
		}
		else {
			returnFound = true;
			if(!statement.getExpr().struct.equals(currentMethodDecl.getType())) {
				report_error("Povratna vrednost funkcije " + currentMethodDecl.getName() + " nije istog tipa kao izraz Expr. Greska", statement);
			}
		}
	}
	
	public void visit(ReadStatement statement) {
		Obj obj = statement.getDesignator().obj;
		if(obj.getKind() == Obj.Var || obj.getKind() == Obj.Fld || obj.getKind() == Obj.Elem) {
			if(!(obj.getType() == TabExtended.intType || obj.getType() == TabExtended.boolType || obj.getType() == TabExtended.charType)) {
				report_error("Designator nije tipa int, char ili bool. Greska", statement);
			}
		}
		else {
			report_error("Designator ne oznacava promenljivu, element niza ili polje unutar objekta. Greska", statement);
		}
	}
	
	public void visit(PrintStatement statement) {
		Struct struct = statement.getExpr().struct;
		if(!(struct == TabExtended.intType ||struct == TabExtended.boolType || struct == TabExtended.charType)) {
			report_error("Designator nije tipa int, char ili bool. Greska", statement);
		}
	}

	public void visit(NumConstPrintStatement statement) {
		Struct struct = statement.getExpr().struct;
		if(!(struct == TabExtended.intType || struct == TabExtended.boolType || struct == TabExtended.charType)) {
			report_error("Designator nije tipa int, char ili bool. Greska", statement);
		}
	}
	
	public void visit(HasIfBlockStart statement) {
		if(statement.getCondition().struct != TabExtended.boolType) {
			report_error("Uslov u if upitu nije tipa bool. Greska", statement);
		}
	}
	
	public void visit(ForLoopBegin statement) {
		forLoopCallStack.push(statement.getLine());
	}
	
	public void visit(HasForSecondParam statement) {
		if(statement.getCondition().struct != TabExtended.boolType) {
			report_error("Uslov u okviru for petlje nije tipa bool. Greska", statement);
		}
	}
	
	public void visit(ForStatement statement) {
		forLoopCallStack.pop();
	}
	
	
	
	//DesignatorStatement obrada
	
	public void visit(AssignopDesignatorStatement des) {
		
		Obj obj = des.getDesignatorStatementName().getDesignator().obj;

		if(obj.getKind() == Obj.Con) {
			des.struct = TabExtended.noType;
			report_error("Konstanta " + obj.getName() + " ne moze biti leva strana dodele. Greska", des);
			return;
		}

		if(obj.getKind() == Obj.Meth) {
			des.struct = TabExtended.noType;
			report_error("Metoda " + obj.getName() + " ne moze biti leva strana dodele. Greska", des);
			return;
		}

		if(obj.getKind() == Obj.Var || obj.getKind() == Obj.Fld || obj.getKind() == Obj.Elem) {
			if(des.getExpr().struct.assignableTo(obj.getType())) {
				des.struct = obj.getType();
			}
			else {
				des.struct = TabExtended.noType;
				report_error("Expr nije kompatibilan pri dodeli sa tipom neterminala Designator. Greska", des);
			}
		}
		else {
			des.struct = TabExtended.noType;
			report_error("Designator ne oznacava promenljivu, element niza ili polje unutar objekta. Greska", des);
		}
	}
	
	public void visit(PlusPlusDesignatorStatement des) {
		Obj obj = des.getDesignatorStatementName().getDesignator().obj;
		if(obj.getKind() == Obj.Var || obj.getKind() == Obj.Fld || obj.getKind() == Obj.Elem) {
			if(obj.getType() == TabExtended.intType) {
				des.struct = obj.getType();
			}
			else {
				des.struct = TabExtended.noType;
				report_error("Designator nije tipa int. Greska", des);
			}
		}
		else {
			des.struct = TabExtended.noType;
			report_error("Designator ne oznacava promenljivu, element niza ili polje unutar objekta. Greska", des);
		}
	}
	
	public void visit(MinusMinusDesignatorStatement des) {
		Obj obj = des.getDesignatorStatementName().getDesignator().obj;
		if(obj.getKind() == Obj.Var || obj.getKind() == Obj.Fld || obj.getKind() == Obj.Elem) {
			if(obj.getType() == TabExtended.intType) {
				des.struct = obj.getType();
			}
			else {
				des.struct = TabExtended.noType;
				report_error("Designator nije tipa int. Greska", des);
			}
		}
		else {
			des.struct = TabExtended.noType;
			report_error("Designator ne oznacava promenljivu, element niza ili polje unutar objekta. Greska", des);
		}
	}
	
	public void visit(NoActParsDesignatorStatement des) {
		Obj obj = des.getDesignatorStatementName().getDesignator().obj;
		int counter = methodCallStackCounter.pop();
		methodCallStackObj.pop();
		
		if(obj.getKind() != Obj.Meth) {// Provera da li je metoda 
			report_error("Designator " + obj.getName() + " nije funkcija. Greska", des);
			des.struct = TabExtended.noType;
		}
		else if(!checkGlobalScope(obj)) {// Provera da li je metoda globalna
			report_error("Designator " + obj.getName() + " nije globalna funkcija. Greska", des);
			des.struct = TabExtended.noType;
		}
		else if(counter != obj.getLevel()) {
			report_error("Funkcija " + obj.getName() + " zahteva " + obj.getLevel() + " parametra a dobila je " + counter + " parametra. Greska", des);
			des.struct = TabExtended.noType;
		}
		else {
			des.struct = obj.getType();
		}
	}
	
	public void visit(ActParsDesignatorStatement des) {
		Obj obj = des.getDesignatorStatementName().getDesignator().obj;
		int counter = methodCallStackCounter.pop();
		methodCallStackObj.pop();
		
		if(obj.getKind() != Obj.Meth) {// Provera da li je metoda 
			report_error("Designator " + obj.getName() + " nije funkcija. Greska", des);
			des.struct = TabExtended.noType;
		}
		else if(!checkGlobalScope(obj)) {// Provera da li je metoda globalna
			report_error("Designator " + obj.getName() + " nije globalna funkcija. Greska", des);
			des.struct = TabExtended.noType;
		}
		else if(counter != obj.getLevel()) {
			report_error("Funkcija " + obj.getName() + " zahteva " + obj.getLevel() + " parametra a dobila je " + counter + " parametra. Greska", des);
			des.struct = TabExtended.noType;
		}
		else {
			des.struct = obj.getType();
		}
	}
	
	
	public void visit(DesignatorStatementName name) {
		methodCallStackObj.push(name.getDesignator().obj);
		methodCallStackCounter.push(0);
	}
	
	
	//ActPars obrada
	
	public void visit(MiddleActPars par) {
		
		Obj method = methodCallStackObj.peek();
		Integer counter = methodCallStackCounter.pop();
		ArrayList<Obj> symbolList = new ArrayList<>(method.getLocalSymbols());
		if(counter < method.getLevel() && !symbolList.get(counter).getType().compatibleWith(par.getExpr().struct)) {
			report_error("Parametar broj " + (counter + 1) + " funkcije " + method.getName() + " se ne poklapa sa prosledjenim argumentom. Greska", par);
		}
		
		methodCallStackCounter.push(counter + 1);
	}
	
	public void visit(LastActPars par) {

		Obj method = methodCallStackObj.peek();
		Integer counter = methodCallStackCounter.pop();
		ArrayList<Obj> symbolList = new ArrayList<>(method.getLocalSymbols());
		if(counter < method.getLevel() && !symbolList.get(0).getType().compatibleWith(par.getExpr().struct)) {
			report_error("Parametar broj 1 funkcije " + method.getName() + " se ne poklapa sa prosledjenim argumentom. Greska", par);
		}
		methodCallStackCounter.push(counter + 1);
	}
	
	//Condition obrada
	
	public void visit(RelopCondFact cond) {
		Struct s1 = cond.getNoTernaryExpr().struct;
		Struct s2 = cond.getNoTernaryExpr1().struct;
		if(!s1.compatibleWith(s2)) {
			report_error("Prvi i drugi izraz Expr uslova CondFact nisu kompatibilni.", cond);
			cond.struct = TabExtended.noType;
		}
		else if((s1 == TabExtended.intArrayType || s1 == TabExtended.boolArrayType || s2 == TabExtended.charArrayType) 
				&& (!(cond.getRelop() instanceof RelopEqualEqual) && !(cond.getRelop() instanceof RelopNotEqual))) {//TODO proveri ovaj uslov
			report_error("Uz simbole tipa niza mogu se naci samo operatori == i !=. Greska", cond);
			cond.struct = TabExtended.noType;
		}
		else {
			cond.struct = TabExtended.boolType;
		}
	}
	
	public void visit(ExprCondFact cond) {
		cond.struct = cond.getNoTernaryExpr().struct;
	}
	
	public void visit(MiddleCondTerm cond) {
		Struct s1 = cond.getCondFact().struct;
		Struct s2 = cond.getCondTerm().struct;
		if(!s1.equals(s2)) {
			report_error("CondTerm i CondFact nisu kompatibilni.", cond);//TODO proveri jel treba compatible with
			cond.struct = TabExtended.noType;
		}
		else {
			cond.struct = TabExtended.boolType;
		}
	}
	
	public void visit(LastCondTerm cond) {
		cond.struct = cond.getCondFact().struct;
	}
	
	public void visit(MiddleCondition cond) {
		Struct s1 = cond.getCondition().struct;
		Struct s2 = cond.getCondTerm().struct;
		if(!s1.equals(s2)) {
			report_error("Condition i CondTerm nisu kompatibilni.", cond);//TODO proveri jel treba compatible with
			cond.struct = TabExtended.noType;
		}
		cond.struct = TabExtended.boolType;
		
	}
	
	public void visit(LastCondition cond) {
		cond.struct = cond.getCondTerm().struct;
	}
	

	//Expr obrada
	
	public void visit(NoTExpr expr) {// Ovo je Expr
		expr.struct = expr.getNoTernaryExpr().struct;
		 
	}
	
	public void visit(HasTExpr expr) {// Ovo je Expr
		expr.struct = expr.getHasTernaryExpr().struct;
	}

	public void visit(NoTernaryExpr expr) {

		expr.struct = expr.getTermList().struct;
	}
	
	public void visit(HasTermList termList) {// Provera za expr

		if(termList.getTerm().struct != TabExtended.intType) {
			report_error("Term nije tipa int. Greska", termList);
			termList.struct = TabExtended.noType;
		}
		else if(termList.getTermList().struct != TabExtended.intType) {
			report_error("TermList nije tipa int. Greska", termList);
			termList.struct = TabExtended.noType;
		}
		else {
			termList.struct = TabExtended.intType;
		}
	}
	
	public void visit(NoTermList termList) {
		termList.struct = termList.getTerm().struct;
	}


	public void visit(MinusNoTermList termList) {
		if(termList.getTerm().struct != TabExtended.intType) {
			termList.struct = TabExtended.noType;
			report_error("Term nije tipa int. Greska", termList);
		}
		else {
			termList.struct = TabExtended.intType;
		}
	}


	public void visit(TernaryExpr expr) {
		
		if(expr.getNoTernaryExpr().struct.equals(expr.getExpr().struct)){
			if(expr.getCondition().struct == TabExtended.boolType) {
				expr.struct = expr.getNoTernaryExpr().struct;
			}
			else {
				expr.struct = TabExtended.noType;
				report_error("Condition nije tipa bool. Greska", expr);
			}
		}
		else {
			expr.struct = TabExtended.noType;
			report_error("Prvi i drugi Expr ternarnog upita nisu istog tipa. Greska", expr);
		}
	}
	

	//Term obrada
	
	public void visit(MiddleTerm term) {
		
		if(term.getTerm().struct != TabExtended.intType) {
			report_error("Term nije tipa int. Greska", term);
			term.struct = TabExtended.noType;
		}
		else if(term.getFactor().struct != TabExtended.intType) {
			report_error("Factor nije tipa int. Greska", term);
			term.struct = TabExtended.noType;
		}
		else {
			term.struct = TabExtended.intType;
		}
	}
	
	public void visit(LastTerm term) {// Provera za expr
		// Ne treba proveravati term da li je int jer 
		term.struct = term.getFactor().struct;
	}
	

	//Factor obrada
	
	public void visit(NumFactor fact) {
		fact.struct = TabExtended.intType;
	}
	
	public void visit(CharFactor fact) {
		fact.struct = TabExtended.charType;
	}
	
	public void visit(BoolFactor fact) {
		fact.struct = TabExtended.boolType;
	}
	
	public void visit(ExprFactor fact) {
		fact.struct = fact.getExpr().struct;
	}
	
	public void visit(DesignatorFactor fact) {
		fact.struct = fact.getDesignator().obj.getType();
	}
	
	// Provera da li su prosledjeni parametri kompatibilni sa param funkcije se vrsi u ActPars koji se proverava nakon designator
	public void visit(EmptyFuncCallFactor fact) {
		Obj obj = fact.getFactorName().getDesignator().obj;
		int counter = methodCallStackCounter.pop();
		methodCallStackObj.pop();
		
		if(obj.getKind() != Obj.Meth) {// Provera da li je metoda 
			report_error("Designator " + obj.getName() + " nije funkcija. Greska", fact);
			fact.struct = TabExtended.noType;
		}
		else if(!checkGlobalScope(obj)) {// Provera da li je metoda globalna
			report_error("Designator " + obj.getName() + " nije globalna funkcija. Greska", fact);//Nepotrebna provera za B nivo 
			fact.struct = TabExtended.noType;
		}
		else if(counter != obj.getLevel()) {
			report_error("Funkcija " + obj.getName() + " zahteva " + obj.getLevel() + " parametra a dobila je " + counter + " parametra. Greska", fact);
			fact.struct = TabExtended.noType;
		}
		else {
			fact.struct = obj.getType();
		}
	}
	
	// Provera da li su prosledjeni parametri kompatibilni sa param funkcije se vrsi u ActPars koji se proverava nakon designator
	public void visit(NonEmptyFuncCallFactor fact) {
		Obj obj = fact.getFactorName().getDesignator().obj;
		int count = methodCallStackCounter.pop();
		methodCallStackObj.pop();
		
		if(obj.getKind() != Obj.Meth) {// Provera da li je metoda 
			report_error("Designator " + obj.getName() + " nije funkcija. Greska", fact);
			fact.struct = TabExtended.noType;
		}
		else if(!checkGlobalScope(obj)) {// Provera da li je metoda globalna
			report_error("Designator " + obj.getName() + " nije globalna funkcija. Greska", fact);
			fact.struct = TabExtended.noType;
		}
		else if(count != obj.getLevel()) {
			report_error("Funkcija " + obj.getName() + " zahteva " + obj.getLevel() + " parametra a dobila je " + count + " parametra. Greska", fact);
			fact.struct = TabExtended.noType;
		}
		else {
			fact.struct = obj.getType();
		}
	}
	
	public void visit(NewArrayFactor fact) {
		if(fact.getExpr().struct != TabExtended.intType) {//Provera da li je expr tipa int
			report_error("Velicina niza [Expr] nije integer. Greska", fact);
			fact.struct = TabExtended.noType;
		}
		else {//Ovde proveravamo i da li imamo tip
			switch(fact.getType().struct.getKind()) {
			case Struct.Int:
				fact.struct = TabExtended.intArrayType;
				break;
			case Struct.Char:
				fact.struct = TabExtended.charArrayType;
				break;
			case Struct.Bool:
				fact.struct = TabExtended.boolArrayType;
				break;
			default:
				fact.struct = TabExtended.noType;
			}
		}
	}
	
	public void visit(FactorName name) {
		methodCallStackObj.push(name.getDesignator().obj);
		methodCallStackCounter.push(0);
	}
	
	
	

	//Designator obrada
	
	public void visit(BaseDesignator des) {
		des.obj = TabExtended.find(des.getBaseName());
		if(des.obj == TabExtended.noObj) {
			report_error("Designator " + des.getBaseName() + " nije deklarisan. Greska", des);
		}
		else {
			report_info("Pretraga prilikom obrade designatora. Info", des, des.obj);
		}
	}

	public void visit(ArrayDesignatorName node){
		Obj arrayObj = TabExtended.find(node.getBaseName());
		node.obj = arrayObj;   // Store the array's Obj

		if(arrayObj == TabExtended.noObj){
			report_error("Designator " + node.getBaseName() + " nije deklarisan. Greska", node);
		}
		else if(arrayObj.getType().getKind() != Struct.Array){
			report_error("Designator " + node.getBaseName() + " nije niz. Greska", node);
			node.obj = TabExtended.noObj;
		}
	}

	public void visit(ArrayDesignator des){

		Obj arrayObj = des.getArrayDesignatorName().obj;

		if(arrayObj == TabExtended.noObj){
			// Array was not found or not an array
			des.obj = TabExtended.noObj;
		}
		else if(des.getExpr().struct != TabExtended.intType){
			report_error("Izraz u okviru [] nije tipa int. Greska", des);
			des.obj = TabExtended.noObj;
		}
		else{
			des.obj = new Obj(Obj.Elem, des.getArrayDesignatorName().getBaseName(),arrayObj.getType().getElemType());
		}
	}

	public void visit(PeriodDesignator des){
		Obj obj = TabExtended.find(des.getBaseName());

		if(obj == TabExtended.noObj){
			report_error("Designator " + des.getBaseName() + " nije deklarisan. Greska", des);
			des.obj = TabExtended.noObj;
		}
		else if(obj.getType().getKind() != Struct.Array) {
			report_error("Designator " + des.getBaseName() + " nije niz. Greska", des);
			des.obj = TabExtended.noObj;
		}
		else{
			report_info("Pretraga prilikom obrade designatora. Info", des, obj);

			PeriodElem elem = des.getPeriodElem();

			if(elem instanceof FindAnyPeriodElem || elem instanceof MapPeriodElem){
				Struct elemType = obj.getType().getElemType();
				if(elemType != TabExtended.intType &&
						elemType != TabExtended.charType &&
						elemType != TabExtended.boolType) {
					report_error("Niz " + des.getBaseName() + " nije ugrađenog tipa. Greska", des);
					des.obj = TabExtended.noObj;
					return;
				}
			}

			if(elem instanceof LenPeriodElem){
				// .length → int constant
				des.obj = new Obj(Obj.Con, des.getBaseName() + ".length", TabExtended.intType);
				elem.obj = obj;
				report_info("Pristup duzini niza " + des.getBaseName() + ". Info", des, des.obj);
			}
			else if(elem instanceof FindAnyPeriodElem){
				FindAnyPeriodElem findAny = (FindAnyPeriodElem) elem;
				Struct elemType = obj.getType().getElemType();
				Struct exprType = findAny.getExpr().struct;

				if(!exprType.compatibleWith(elemType)){
					report_error("Expr nije kompatibilan sa tipom elemenata niza. Greska", findAny);
				}
				des.obj = new Obj(Obj.Con, des.getBaseName() + ".findAny", TabExtended.boolType);
				report_info("Poziv funkcije findAny nad nizom " + des.getBaseName() + ". Info", des, des.obj);
			}
			else if(elem instanceof MapPeriodElem){
				MapPeriodElem mapElem = (MapPeriodElem) elem;
				Struct elemType = obj.getType().getElemType();

				Obj identObj = TabExtended.find(mapElem.getMapElem());
				if(identObj != TabExtended.noObj) {
					if(!identObj.getType().compatibleWith(elemType)){
						report_error("Identifikator " + mapElem.getMapElem() + " nije istog tipa kao elementi niza. Greska", mapElem);
					}
					else{
						report_info("Pretraga prilikom obrade map identifikatora. Info", mapElem, identObj);
					}
				}

				Struct exprType = mapElem.getExpr().struct;
				if(!exprType.compatibleWith(elemType)){
					report_error("Expr nije kompatibilan sa tipom elemenata niza. Greska", mapElem);
				}

				// map returns array of same type
				des.obj = new Obj(Obj.Con, des.getBaseName() + ".map", obj.getType());
				report_info("Poziv funkcije map nad nizom " + des.getBaseName() + ". Info", des, des.obj);
			}
		}
	}

	public void visit(ErrorVarDeclElem elem) {}
	public void visit(ErrorLastVarDeclElem elem) {}
	public void visit(ErrorFormParsElem elem) {}
	public void visit(ErrorLastFormParsElem elem) {}
	public void visit(ErrorDesignatorStatement stmt) {}
	public void visit(ErrorIfBlockStart stmt) {}


	
    public boolean passed() {
    	return !errorDetected;
    }
    
}