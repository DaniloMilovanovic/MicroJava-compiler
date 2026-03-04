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
	
	Obj currentMethod = null;
	boolean returnFound = false;
	boolean errorDetected = false;
	int nVars;
	
	Type currType;//Trenutni tip za constDecl i varDecl
	
	int currPos = 0; //Trenutni indeks enuma
	String currEnumName;
	ArrayList<Integer> enumElems = new ArrayList<>();
	
	Logger log = Logger.getLogger(getClass());
	
	private Obj obj;
	
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
	private boolean checkVarNameConstraints(String name) {
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
		if(checkVarNameConstraints(vDeclElem.getName())) {//Simbol nije deklarisan u trenutnom opsegu
			TabExtended.insert(Obj.Var, vDeclElem.getName(), currType.struct);
			report_info("Deklarisana promenljiva: " + vDeclElem.getName() + ". Info", vDeclElem);
		}
		else {//Simbol je vec deklarisan u trenutnom opsegu
			report_error("Promenljiva " + vDeclElem.getName() + " je vec deklarisana. Greska", vDeclElem);
		}
		
	}

	public void visit (NoBrackLastVarDeclElem vDeclElem) {
		if(checkVarNameConstraints(vDeclElem.getName())) {//Simbol nije deklarisan u trenutnom opsegu
			TabExtended.insert(Obj.Var, vDeclElem.getName(), currType.struct);
			report_info("Deklarisana promenljiva: " + vDeclElem.getName() + ". Info", vDeclElem);
		}
		else {//Simbol je vec deklarisan u trenutnom opsegu
			report_error("Promenljiva " + vDeclElem.getName() + " je vec deklarisana. Greska", vDeclElem);
		}
	}
	
	public void visit (BrackVarDeclElem vDeclElem) {
		
		if(checkVarNameConstraints(vDeclElem.getName())) {//Simbol nije deklarisan u trenutnom opsegu
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
		
		if(checkVarNameConstraints(vDeclElem.getName())) {//Simbol nije deklarisan u trenutnom opsegu
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

			if(checkVarNameConstraints(currEnumName + "." + eDeclElem.getName())){// Konstanta ne sme vise puta biti deklarisana u okviru istog enuma.
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

			if(checkVarNameConstraints(currEnumName + "." + eDeclElem.getName())){// Konstanta ne sme vise puta biti deklarisana u okviru istog enuma.
				
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
	
    public boolean passed(){
    	return !errorDetected;
    }
    
}
