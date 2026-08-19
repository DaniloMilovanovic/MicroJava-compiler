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

    public void visit(NumConstPrintStatement printStatement){

        Code.loadConst(printStatement.getNumConst().getVal());
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
        }
        else{
            Code.put(Code.read);
            Code.store(obj);
        }
    }

    //DESIGNATOR
    public void visit(BaseDesignator baseDesignator){
        Code.load(baseDesignator.obj);
    }

    public void visit(AssignopDesignatorStatement node){
        Code.store(node.getDesignatorStatementName().getDesignator().obj);
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
    public void visit(MinusExpr minusExpr){
        Code.put(Code.neg);
    }

    public void visit(HasTermList hasTermList){
        if(hasTermList.getAddop() instanceof AddopMinus){
            Code.put(Code.sub);
        }
        else if(hasTermList.getAddop() instanceof AddopPlus){
            Code.put(Code.add);
        }
    }

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