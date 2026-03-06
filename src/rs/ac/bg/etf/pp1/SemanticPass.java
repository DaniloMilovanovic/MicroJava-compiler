package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticPass extends VisitorAdaptor {

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
	
	Type currType;//Trenutni tip za constDecl i varDecl
	
	int currPos = 0; //Trenutni indeks enuma
	String currEnumName; //Trenutni naziv enuma
	ArrayList<Integer> enumElems = new ArrayList<>(); //Svi brojevi iskorisceni za enum
	
	Obj currentMethod = null; //Trenutna metoda koja se obradjuje
	boolean returnFound = false; //Da li je pronadjen return u telu metode
	
	int formalParsCount = 0; //Broj formalnih parametara za opis metode
	
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
		return elem == null;
	}

	private boolean containsEnumConstant(int num) {
		for(int el: enumElems) {
			if(el == num) {
				return true;
			}
		}
		return false;
	}
	
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
	
	//Program obrada
	
	public void visit(ProgName pName) {
		pName.obj = TabExtended.insert(Obj.Prog, pName.getName(), TabExtended.noType);
		TabExtended.openScope();
		report_info("Zapoceta je obrada programa: " + pName.getName() + ". Info", pName);
	}
	
	public void visit(Program prog) {
		Obj obj = prog.getProgName().obj;
		TabExtended.chainLocalSymbols(obj);
		TabExtended.closeScope();
		report_info("Zavrsena je obrada programa: " + prog.getProgName().getName() + ". Info", prog);
	}
	
	//Type obrada
	
	public void visit(Type type){
    	Obj typeNode = TabExtended.find(type.getTypeName());
    	if(typeNode == TabExtended.noObj){
    		report_error("Nije pronadjen tip " + type.getTypeName() + " u tabeli simbola! Greska", null);
    		type.struct = TabExtended.noType;
    	}else{
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
		if(checkCurrentScope(vDeclElem.getName())) {//Simbol nije deklarisan u trenutnom opsegu
			TabExtended.insert(Obj.Var, vDeclElem.getName(), currType.struct);
			report_info("Deklarisana promenljiva: " + vDeclElem.getName() + ". Info", vDeclElem);
		}
		else {//Simbol je vec deklarisan u trenutnom opsegu
			report_error("Promenljiva " + vDeclElem.getName() + " je vec deklarisana. Greska", vDeclElem);
		}
		
	}

	public void visit (NoBrackLastVarDeclElem vDeclElem) {
		if(checkCurrentScope(vDeclElem.getName())) {//Simbol nije deklarisan u trenutnom opsegu
			TabExtended.insert(Obj.Var, vDeclElem.getName(), currType.struct);
			report_info("Deklarisana promenljiva: " + vDeclElem.getName() + ". Info", vDeclElem);
		}
		else {//Simbol je vec deklarisan u trenutnom opsegu
			report_error("Promenljiva " + vDeclElem.getName() + " je vec deklarisana. Greska", vDeclElem);
		}
	}
	
	public void visit (BrackVarDeclElem vDeclElem) {
		
		if(checkCurrentScope(vDeclElem.getName())) {//Simbol nije deklarisan u trenutnom opsegu
			Struct arrayType = getArrayStruct(currType.struct);
			if(arrayType == Tab.noType) {//Ne postoji struktura niza za nas tip, verovatno nepotrebna provera
				report_error("Nije pronadjen tip niza " + currType.getTypeName() + " u tabeli simbola! Greska", null);
				
			}
			else {//Postoji struktura niza za nas tip
				TabExtended.insert(Obj.Var, vDeclElem.getName(), arrayType);
				report_info("Deklarisana promenljiva: " + vDeclElem.getName() + ". Info", vDeclElem);
			}
		}
		else {//Simbol je vec deklarisan u trenutnom opsegu
			report_error("Promenljiva " + vDeclElem.getName() + " je vec deklarisana. Greska", vDeclElem);
		}
	}
	
	public void visit (BrackLastVarDeclElem vDeclElem) {
		
		if(checkCurrentScope(vDeclElem.getName())) {//Simbol nije deklarisan u trenutnom opsegu
			Struct arrayType = getArrayStruct(currType.struct);
			if(arrayType == Tab.noType) {//Ne postoji struktura niza za nas tip, verovatno nepotrebna provera
				report_error("Nije pronadjen tip niza " + currType.getTypeName() + " u tabeli simbola! Greska", null);
				
			}
			else {//Postoji struktura niza za nas tip
				TabExtended.insert(Obj.Var, vDeclElem.getName(), arrayType);
				report_info("Deklarisana promenljiva: " + vDeclElem.getName() + ". Info", vDeclElem);
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
				report_info("Deklarisana konstanta: " + cDecl.getName() +" sa vrednoscu " + cDecl.getNumConst().getVal() + ". Info", cDecl);
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
				report_info("Deklarisana konstanta: " + cDecl.getName() +" sa vrednoscu " + cDecl.getCharConst().getVal() + ". Info", cDecl);
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
					report_info("Deklarisana konstanta: " + cDecl.getName() + " sa vrednoscu True. Info", cDecl);
				}
				else {
					constant.setAdr(0);
					report_info("Deklarisana konstanta: " + cDecl.getName() + " sa vrednoscu False. Info", cDecl);
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
	
	//Enum obrada
	
	public void visit(EnumDeclName eDeclName) {
		currPos = 0;
		currEnumName = eDeclName.getName();
		enumElems.clear();
		report_info("Zapoceta je obrada enum-a: " + eDeclName.getName() + ". Info", eDeclName);
	}
	
	public void visit(EnumDecl eDecl) {
		report_info("Zavrsena je obrada enum-a: " + eDecl.getEnumDeclName().getName() + ". Info", eDecl);
	}
	
	public void visit(NoValEnumDeclElem eDeclElem) {
		if(!containsEnumConstant(currPos)) {// Konstanta sa istim brojem nije deklarisana

			if(checkCurrentScope(currEnumName + "." + eDeclElem.getName())){// Konstanta ne sme vise puta biti deklarisana u okviru istog enuma.
				Obj obj = TabExtended.insert(Obj.Con, currEnumName + "." + eDeclElem.getName(), TabExtended.intType);
				obj.setAdr(currPos);
				enumElems.add(currPos);
				currPos++;
			}
			else {// Konstanta je vec deklarisana
				report_error("Konstanta tipa enum " + currEnumName + "." + eDeclElem.getName() + " je vec deklarisana. Greska", eDeclElem);
			}
		}
		else {// Konstanta sa istim brojem je prethodno deklarisana
			report_error("Konstanta tipa enum sa vrednoscu " + currPos + " je vec deklarisana. Greska", eDeclElem);
		}
	}
	
	public void visit(ValEnumDeclElem eDeclElem) {// Konstanta je uvek broj po definiciji leksera
		currPos = eDeclElem.getNumConst().getVal();
		if(!containsEnumConstant(currPos)) {// Konstanta sa istim brojem nije deklarisana

			if(checkCurrentScope(currEnumName + "." + eDeclElem.getName())){// Konstanta ne sme vise puta biti deklarisana u okviru istog enuma.
				
				Obj obj = TabExtended.insert(Obj.Con, currEnumName + "." + eDeclElem.getName(), TabExtended.intType);
				obj.setAdr(eDeclElem.getNumConst().getVal());
				enumElems.add(currPos);
				currPos++;
			}
			else {// Konstanta je vec deklarisana
				report_error("Konstanta tipa enum " + currEnumName + "." + eDeclElem.getName() + " je vec deklarisana. Greska", eDeclElem);
			}
		}
		else {// Konstanta sa istim brojem je prethodno deklarisana
			report_error("Konstanta tipa enum sa vrednoscu " + currPos + " je vec deklarisana. Greska", eDeclElem);
		}
	}
	
	
	//MethodDecl obrada
	
	//TODO:
	
	//DesignatorStatement ::= Designator LPAREN ActPars RPAREN
	//chr(e); e mora biti izraz tipa int.
	//ord(c); c mora biti tipa char.
	//len(a); a mora biti niz ili znakovni niz
	
	public void visit(TMethodDecl mDecl) {
    	currentMethod = Tab.insert(Obj.Meth, mDecl.getName(), mDecl.getType().struct);
    	mDecl.obj = currentMethod;
    	formalParsCount = 0;
    	Tab.openScope();
		report_info("Obradjuje se funkcija " + mDecl.getName() + ". Info", mDecl);
	}
	
	public void visit(VMethodDecl mDecl) {
    	currentMethod = Tab.insert(Obj.Meth, mDecl.getName(), TabExtended.noType);
    	mDecl.obj = currentMethod;
    	formalParsCount = 0;
    	Tab.openScope();
		report_info("Obradjuje se funkcija " + mDecl.getName() + ". Info", mDecl);
	}
	
	public void visit(MethodDecl mDecl) {
		if(currentMethod.getName().equals("main") && formalParsCount > 0) {
			if(formalParsCount == 1) {
				report_error("Metoda main ima 1 formalni parametar sto je vece od 0! Greska", mDecl);
			}
			else {
				report_error("Metoda main ima " + formalParsCount + " formalna parametara sto je vece od 0! Greska", mDecl);
			}
		}
		if(!returnFound && currentMethod.getType() != Tab.noType){//TODO proveri u izrazima jel postoji return!!!
			report_error("Funkcija " + currentMethod.getName() + " nema return iskaz! Greska", mDecl);
    	}
		currentMethod.setLevel(formalParsCount);
    	Tab.chainLocalSymbols(currentMethod);
    	Tab.closeScope();
    	
    	returnFound = false;
    	currentMethod = null;
	}
	
	//FromPars obrada
	
	public void visit(NoBrackFormParsElem fPars) {
		if(checkCurrentScope(fPars.getName())) {// Simbol nije deklarisan u trenutnom opsegu
			Obj formPar = TabExtended.insert(Obj.Var, fPars.getName(), fPars.getType().struct);
			report_info("Deklarisan formalni parametar: " + fPars.getName() + ". Info", fPars);
			formPar.setAdr(formalParsCount);
			formalParsCount++;
		}
		else {// Simbol je vec deklarisan u trenutnom opsegu
			report_error("Formalni parametar " + fPars.getName() + " je vec deklarisana. Greska", fPars);
		}
	}
	
	public void visit(NoBrackLastFormParsElem fPars) {
		if(checkCurrentScope(fPars.getName())) {// Simbol nije deklarisan u trenutnom opsegu
			Obj formPar = TabExtended.insert(Obj.Var, fPars.getName(), fPars.getType().struct);
			report_info("Deklarisan formalni parametar: " + fPars.getName() + ". Info", fPars);
			formPar.setAdr(formalParsCount);
			formalParsCount++;
		}
		else {// Simbol je vec deklarisan u trenutnom opsegu
			report_error("Formalni parametar " + fPars.getName() + " je vec deklarisana. Greska", fPars);
		}
	}
	
	public void visit(BrackFormParsElem fPars) {
		if(checkCurrentScope(fPars.getName())) {// Simbol nije deklarisan u trenutnom opsegu
			Struct arrayType = getArrayStruct(fPars.getType().struct);
			if(arrayType == Tab.noType) {// Ne postoji struktura niza za nas tip, verovatno nepotrebna provera
				report_error("Nije pronadjen tip niza " + fPars.getType().getTypeName() + " u tabeli simbola! Greska", null);
				
			}
			else {// Postoji struktura niza za nas tip
				Obj formPar = TabExtended.insert(Obj.Var, fPars.getName(), arrayType);
				report_info("Deklarisana formalni parametar: " + fPars.getName() + ". Info", fPars);
				formPar.setAdr(formalParsCount);
				formalParsCount++;
			}
		}
		else {// Simbol je vec deklarisan u trenutnom opsegu
			report_error("Formalni parametar " + fPars.getName() + " je vec deklarisana. Greska", fPars);
		}
	}
	public void visit(BrackLastFormParsElem fPars) {
		if(checkCurrentScope(fPars.getName())) {// Simbol nije deklarisan u trenutnom opsegu
			Struct arrayType = getArrayStruct(fPars.getType().struct);
			if(arrayType == Tab.noType) {// Ne postoji struktura niza za nas tip, verovatno nepotrebna provera
				report_error("Nije pronadjen tip niza " + fPars.getType().getTypeName() + " u tabeli simbola! Greska", null);
				
			}
			else {// Postoji struktura niza za nas tip
				Obj formPar = TabExtended.insert(Obj.Var, fPars.getName(), arrayType);
				report_info("Deklarisana formalni parametar: " + fPars.getName() + ". Info", fPars);
				formPar.setAdr(formalParsCount);
				formalParsCount++;
			}
		}
		else {// Simbol je vec deklarisan u trenutnom opsegu
			report_error("Formalni parametar " + fPars.getName() + " je vec deklarisana. Greska", fPars);
		}
	}
	
	//DesignatorStatement obrada
	
	public void visit(AssignopDesignatorStatement des) {
		Obj obj = des.getDesignatorStatementName().getDesignator().obj;
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
		if(obj.getKind() != Obj.Meth) {// Provera da li je metoda 
			report_error("Designator " + obj.getName() + " nije funkcija. Greska", des);
			des.struct = TabExtended.noType;
		}
		else if(obj.getLevel() != 0) {// Provera da li je metoda globalna
			report_error("Designator " + obj.getName() + " nije globalna funkcija. Greska", des);
			des.struct = TabExtended.noType;
		}
		else if(obj.getLevel() != 0) {
			report_error("Broj formalnih parametara funkcije je veci od 0 ali nije prosledjen nijedan parametar. Greska", des);
			des.struct = TabExtended.noType;
		}
		else {
			des.struct = obj.getType();
		}
	}
	
	public void visit(ActParsDesignatorStatement des) {
		Obj obj = des.getDesignatorStatementName().getDesignator().obj;
		if(obj.getKind() != Obj.Meth) {// Provera da li je metoda 
			report_error("Designator " + obj.getName() + " nije funkcija. Greska", des);
			des.struct = TabExtended.noType;
		}
		else if(obj.getLevel() != 0) {// Provera da li je metoda globalna
			report_error("Designator " + obj.getName() + " nije globalna funkcija. Greska", des);
			des.struct = TabExtended.noType;
		}
		else {
			des.struct = obj.getType();
		}
	}
	
	public void visit(DesignatorStatementName name) {
		currentMethod = name.getDesignator().obj;
	}
	
	//Designator obrada
	
	public void visit(BaseDesignator des) {
		des.obj = TabExtended.find(des.getBaseName());
		if(des.obj == TabExtended.noObj) {
			report_error("Designator " + des.getBaseName() + " nije deklarisan. Greska", des);
		}
	}

	public void visit(EnumDesignator des) {
		String name = des.getBaseName() + "." + des.getScopeName();
		des.obj = TabExtended.find(name);
		if(des.obj == TabExtended.noObj) {// Provera da li postoji designator u tabeli simbola
			report_error("Designator " + name  + " nije deklarisan. Greska", des);
		}
		else if(des.obj.getKind() != Obj.Con) {// Provera da li postoji designator konstanta
			report_error("Designator " + des.obj.getName()  + " nije konstanta nabrajanja. Greska", des);
		}
	}
	
	public void visit(LengthDesignator des) {
		des.obj = TabExtended.find(des.getBaseName());
		if(des.obj == TabExtended.noObj) {// Provera da li postoji designator u tabeli simbola
			report_error("Designator " + des.getBaseName() + " nije deklarisan. Greska", des);
		}
		else if(des.obj.getType().getKind() != Struct.Array) {// Provera da li je simbol niz
			report_error("Designator " + des.obj.getName()  + " nije niz. Greska", des);
		}
		des.obj = new Obj(Obj.Con, des.getBaseName(), TabExtended.intType);
	}
	
	public void visit(ArrayDesignator des) {
		Obj obj = TabExtended.find(des.getBaseName());
		if(obj == TabExtended.noObj) {// Provera da li postoji designator u tabeli simbola
			report_error("Designator " + des.getBaseName() + " nije deklarisan. Greska", des);
		} 
		else if(obj.getType().getKind() != Struct.Array) {// Provera da li je simbol niz
			report_error("Designator " + obj.getName()  + " nije niz. Greska", des);
		}
		else if (des.getExpr().struct != TabExtended.intType) {// Izraz nije int
			report_error("Izraz u okviru [] nije tipa int. Greska", des);
		}
		else {
			des.obj = new Obj(Obj.Elem, des.getBaseName(), obj.getType().getElemType()); //Element tipa Elem, isto ime kao niz, struct je tip niza
		}//TODO dodatno proveri jel treba da se postavi u addr mozda pozicija u okviru niza
		
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
		fact.struct = fact.getFactorName().getDesignator().obj.getType();
	}
	
	
	public void visit(EmptyFuncCallFactor fact) {
		Obj obj = fact.getFactorName().getDesignator().obj;
		if(obj.getKind() != Obj.Meth) {// Provera da li je metoda 
			report_error("Designator " + obj.getName() + " nije funkcija. Greska", fact);
			fact.struct = TabExtended.noType;
		}
		else if(fact.getFactorName().getDesignator().obj.getLevel() != 0) {// Provera da li je metoda globalna
			report_error("Designator " + obj.getName() + " nije globalna funkcija. Greska", fact);
			fact.struct = TabExtended.noType;
		}
		else if(obj.getLevel() != 0) {
			report_error("Broj formalnih parametara funkcije je veci od 0 ali nije prosledjen nijedan parametar. Greska", fact);
			fact.struct = TabExtended.noType;
		}
		else {
			obj.getType();
		}
	}
	
	// Provera da li su prosledjeni parametri kompatibilni sa param funkcije se vrsi u ActPars koji se proverava nakon designator
	public void visit(NonEmptyFuncCallFactor fact) {
		//TODO check params
		Obj obj = fact.getFactorName().getDesignator().obj;
		if(obj.getKind() != Obj.Meth) {// Provera da li je metoda 
			report_error("Designator " + obj.getName() + " nije funkcija. Greska", fact);
			fact.struct = TabExtended.noType;
		}
		else if(obj.getLevel() != 0) {// Provera da li je metoda globalna
			report_error("Designator " + obj.getName() + " nije globalna funkcija. Greska", fact);
			fact.struct = TabExtended.noType;
		}
		else {
			fact.struct = obj.getType();
		}
	}
	
	public void visit(NewArrayFactor fact) {
		if(fact.getExpr().struct != TabExtended.intType) {//Provera da li je expr tipa int
			report_error("Izraz Expr nije integer. Greska", fact);
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
		currentMethod = name.getDesignator().obj;
	}
	
	//Term obrada
	
	public void visit(MiddleTerm term) {
		
		if(term.getTerm().struct != TabExtended.intType) {
			report_error("Term nije tipa int. Greska", term);
		}
		else if(term.getFactor().struct != TabExtended.intType) {
			report_error("Factor nije tipa int. Greska", term);
		}
		else {
			term.struct = term.getTerm().struct;
		}
	}
	
	public void visit(LastTerm term) {// Provera za expr
		// Ne treba proveravati term da li je int jer 
		term.struct = term.getFactor().struct;
	}
	
	public void visit(HasTermList termList) {// Provera za expr

		if(termList.getTerm().struct != TabExtended.intType) {
			report_error("Term nije tipa int. Greska", termList);
		}
		else if(termList.getTermList().struct != TabExtended.intType) {
			report_error("TermList nije tipa int. Greska", termList);
		}
		else {
			termList.struct = termList.getTerm().struct;
		}
	}
	
	public void visit(NoTermList termList) {
		termList.struct = termList.getTerm().struct;
	}
	
	//Expr obrada
	
	public void visit(NoTExpr expr) {// Ovo je Expr
		expr.struct = expr.getNoTernaryExpr().struct;
	}
	
	public void visit(HasTExpr expr) {// Ovo je Expr
		expr.struct = expr.getHasTernaryExpr().struct;
	}
	
	public void visit(MinusExpr expr) {
		if(expr.getTermList().struct != TabExtended.intType) {
			expr.struct = TabExtended.noType;
			report_error("TermList nije tipa int. Greska", expr);
		}
		else {
			expr.struct = expr.getTermList().struct;	
		}
	}
	
	public void visit(NoMinusExpr expr) {
		expr.struct = expr.getTermList().struct;
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
	
	//TODO condition i svi nizi delovi moraju da provere jel su bool
	
    public boolean passed() {
    	return !errorDetected;
    }
    
    //TODO testiraj Factor, Term, expr, DesignatorStatement i ActPars
    
}
