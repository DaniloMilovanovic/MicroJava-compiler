package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeGenerator extends VisitorAdaptor {

    private int mainPc = -1;
    private Obj currentMethod = null;
    private int paramsCount = 0;
    private int localsCount = 0;

    int getMainPc(){
        return mainPc;
    }

    public void visit(Program p){
        int dataSize = 0;
        Obj pObj = p.getProgName().obj;
        for(Obj obj: pObj.getLocalSymbols()){
            if(obj.getKind() == Obj.Var){
                dataSize++;
            }
        }
        Code.dataSize = dataSize;
    }

    //METHODS
    public void visit(TMethodDecl mDecl){

        paramsCount = mDecl.obj.getLevel();
        localsCount = mDecl.obj.getLocalSymbols().size() - paramsCount;
        currentMethod = mDecl.obj;
        currentMethod.setAdr(Code.pc);

        Code.put(Code.enter);
        Code.put(paramsCount);
        Code.put(localsCount);
    }

    public void visit(VMethodDecl mDecl){
        if(mDecl.getName().equals("main")){
            mainPc = Code.pc;
        }
        paramsCount = mDecl.obj.getLevel();
        localsCount = mDecl.obj.getLocalSymbols().size() - paramsCount;
        currentMethod = mDecl.obj;
        currentMethod.setAdr(Code.pc);

        Code.put(Code.enter);
        Code.put(paramsCount);
        Code.put(localsCount);
    }

    public void visit(MethodDecl mDecl){
        Code.put(Code.exit);
        Code.put(Code.return_);
        currentMethod = null;
    }

    //STATEMENTS
    public void visit(PrintStatement printStatement){

        int kind = printStatement.getExpr().struct.getKind();

        if(kind == Struct.Int || kind == Struct.Bool){
            Code.loadConst(0);
            Code.put(Code.print);
        }
        else if(kind == Struct.Char){
            Code.loadConst(1);
            Code.put(Code.bprint);
        }
    }

    public void visit(ReturnValueStatement returnStatement){
        Code.put(Code.exit);
        Code.put(Code.return_);
        currentMethod = null;
    }

    public void visit(ReturnStatement returnStatement){
        Code.put(Code.exit);
        Code.put(Code.return_);
        currentMethod = null;
    }

    public void visit(NumConstPrintStatement printStatement){

        // DON'T push the width - it's already pushed by visit(NumConst)!

        int kind = printStatement.getExpr().struct.getKind();

        if(kind == Struct.Int || kind == Struct.Bool){
            Code.put(Code.print);
        }
        else if(printStatement.getExpr().struct.getKind() == Struct.Char){
            Code.put(Code.bprint);
        }
    }

    public void visit(ReadStatement readStatement){
        Obj obj = readStatement.getDesignator().obj;
        if(obj.getType().getKind() == Struct.Char){
            Code.put(Code.bread);
            Code.store(obj);

            Code.put(Code.bread);       // Read the newline \r
            Code.put(Code.pop);         //discard it
            Code.put(Code.bread);       //read \n
            Code.put(Code.pop);         //discard it
        }
        else{
            Code.put(Code.read);
            Code.store(obj);
        }
    }

    //DESIGNATOR
    public void visit(BaseDesignator baseDesignator){
        Obj obj = baseDesignator.obj;

        if (obj.getKind() == Obj.Meth){ //Already handled by function call visitors
            return;
        }

        Code.load(obj);
    }
    public void visit(PeriodDesignator node){

        PeriodElem elem = node.getPeriodElem();

        if(elem instanceof LenPeriodElem){

            LenPeriodElem lenElem = (LenPeriodElem) elem;
            Code.load(lenElem.obj);
            Code.put(Code.arraylength);
        }
    }

    public void visit(ArrayDesignatorName node){
        Code.load(node.obj);
    }

    public void visit(ArrayDesignator node){
        //Moramo znati da li se koristi za citanje ili pisanje
        SyntaxNode parent = node.getParent();

        //Ako citamo onda imamo [arr, offset] na steku i samo saljemo aload ili baload.
        if(parent instanceof DesignatorFactor){
            if(node.obj.getType().getKind() == Struct.Char){
                Code.put(Code.baload);
            }
            else{
                Code.put(Code.aload);
            }
        }
        // Ako pisemo onda se ovo razresava u AssignopDesignatorStatement, tada je stek [arr, ind, val] i store
        // funkcija koja nam je data pozove lepo astore ili bastore.
    }

    //DesignatorStatement

    public void visit(AssignopDesignatorStatement node){
        Code.store(node.getDesignatorStatementName().getDesignator().obj);
    }

    public void visit(ActParsDesignatorStatement node){
        Obj meth = node.getDesignatorStatementName().getDesignator().obj;

        if(meth == Tab.ordObj || meth == Tab.chrObj){
            Code.put(Code.pop);//No return value is needed.
            return;
        }
        if(meth == Tab.lenObj){//No return value necessary
            Code.put(Code.arraylength);
            Code.put(Code.pop);
            return;
        }

        int offset = meth.getAdr() - Code.pc;
        Code.put(Code.call);
        Code.put2(offset);

        if(meth.getType() != Tab.noType){
            Code.put(Code.pop);
        }
    }

    public void visit(NoActParsDesignatorStatement node){
        Obj meth = node.getDesignatorStatementName().getDesignator().obj;

        Code.put(Code.call);
        Code.put2(meth.getAdr() - Code.pc + 1);

        if(meth.getType() != Tab.noType){
            Code.put(Code.pop);
        }
    }



    public void visit(PlusPlusDesignatorStatement node){
        Obj obj = node.getDesignatorStatementName().getDesignator().obj;

        Code.load(obj);
        Code.loadConst(1);
        Code.put(Code.add);
        Code.store(obj);
    }

    public void visit(MinusMinusDesignatorStatement node){
        Obj obj = node.getDesignatorStatementName().getDesignator().obj;

        Code.load(obj);
        Code.loadConst(1);
        Code.put(Code.sub);
        Code.store(obj);
    }

    //EXPRESSIONS

    public void visit(MiddleTerm middleTerm){

        if(middleTerm.getMulop() instanceof  MulopStar){
            Code.put(Code.mul);
        }
        else if(middleTerm.getMulop() instanceof  MulopDiv){
            Code.put(Code.div);
        }
        else if(middleTerm.getMulop() instanceof MulopMod){
            Code.put(Code.rem);
        }
    }

    public void visit(HasTermList hasTermList){
        if(hasTermList.getAddop() instanceof AddopMinus){
            Code.put(Code.sub);
        }
        else if(hasTermList.getAddop() instanceof AddopPlus){
            Code.put(Code.add);
        }
    }

    public void visit(MinusNoTermList termList){
        Code.put(Code.neg);
    }


    public void visit(DesignatorFactor designatorFactor){
        //Value already loaded in Designator;
    }

    public void visit(BoolFactor boolFactor){
        //Value already loaded in BoolConst;
    }

    public void visit(NumFactor numFactor){
        //Value already loaded in NumConst;
    }

    public void visit(CharFactor charFactor){
        //Value already loaded in CharConst;
    }

    public void visit(NonEmptyFuncCallFactor node){
        Obj meth = node.getFactorName().getDesignator().obj;

        if (meth == Tab.ordObj || meth == Tab.chrObj) {
            return;//already on the stack
        }
        if (meth == Tab.lenObj) {
            Code.put(Code.arraylength);
            Code.put(Code.pop);
            return;
        }

        int offset = meth.getAdr() - Code.pc;
        Code.put(Code.call);
        Code.put2(offset);
    }

    public void visit(EmptyFuncCallFactor node){
        Obj meth = node.getFactorName().getDesignator().obj;

        Code.put(Code.call);
        Code.put2(meth.getAdr() - Code.pc + 1);
    }

    public void visit(NewArrayFactor node){
        Struct type = node.getType().struct;

        Code.put(Code.newarray);

        if(type == TabExtended.intType){
            Code.put(1);
        }
        else if(type == TabExtended.charType){
            Code.put(0);
        }
        else if(type == TabExtended.boolType){
            Code.put(1);
        }
    }

    //ActPars

    public void visit(LastActPars node) {
        // First argument - Expr already visited, value on stack
        // Nothing to do
    }

    public void visit(MiddleActPars node) {
        // Additional arguments - Expr already visited, value on stack
        // Nothing to do
    }

    //CONSTANTS

    public void visit(NumConst numConst){
        Code.loadConst(numConst.getVal());
    }

    public void visit(CharConst charConst){
        Code.loadConst(charConst.getVal());
    }

    public void visit(TrueBoolConst trueBoolConst){
        Code.loadConst(1);
    }

    public void visit(FalseBoolConst falseBoolConst){
        Code.loadConst(0);
    }



    //CONDITION
}