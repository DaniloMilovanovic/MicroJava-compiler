package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

import java.util.*;

public class CodeGenerator extends VisitorAdaptor {

    private static final int TEMP_SLOTS = 4;

    private int mainPc = -1;
    private Obj currentMethod = null;
    private int paramsCount = 0;
    private int localsCount = 0;

    private Obj[] temp = new Obj[TEMP_SLOTS];

    private Map<SyntaxNode, List<Integer>> falseJumps = new HashMap<SyntaxNode, List<Integer>>();
    private Map<SyntaxNode, List<Integer>> trueJumps = new HashMap<SyntaxNode, List<Integer>>();
    private Map<SyntaxNode, List<Integer>> endJumps = new HashMap<SyntaxNode, List<Integer>>();

    private Stack<ForStatement> currentForLoop = new Stack<ForStatement>();

    private Map<SyntaxNode, Integer> forLoopCondition = new HashMap<SyntaxNode, Integer>();
    private Map<SyntaxNode, Integer> forLoopUpdate = new HashMap<SyntaxNode, Integer>();
    private Map<SyntaxNode, List<Integer>> forLoopBreaks = new HashMap<SyntaxNode, List<Integer>>();

    int checkAddr;
    int falseEndJumpFixup;
    int trueEndJumpFixup;

    private List<Integer> getListForNode(Map<SyntaxNode, List<Integer>> m, SyntaxNode n) {
        List<Integer> l = m.get(n);
        if (l == null) { l = new ArrayList<Integer>(); m.put(n, l); }
        return l;
    }

    private void fixAllForNode(Map<SyntaxNode, List<Integer>> m, SyntaxNode n) {
        List<Integer> addresses = getListForNode(m, n);
        for (int adr : addresses) Code.fixup(adr);
        getListForNode(m, n).clear();
    }

    private void moveList(Map<SyntaxNode, List<Integer>> srcMap, SyntaxNode srcNode,
                          Map<SyntaxNode, List<Integer>> dstMap, SyntaxNode dstNode){
        getListForNode(dstMap, dstNode).addAll(getListForNode(srcMap, srcNode));
        getListForNode(srcMap, srcNode).clear();
    }

    int getMainPc() {
        return mainPc;
    }

    public void visit(Program p) {
        int dataSize = 0;
        Obj pObj = p.getProgName().obj;
        for (Obj obj : pObj.getLocalSymbols()) {
            if (obj.getKind() == Obj.Var) {
                dataSize++;
            }
        }
        Code.dataSize = dataSize;
    }

    //METHODS
    public void visit(TMethodDecl mDecl) {

        paramsCount = mDecl.obj.getLevel();
        localsCount = mDecl.obj.getLocalSymbols().size() - paramsCount;

        for (int i = 0; i < TEMP_SLOTS; i++) {
            temp[i] = new Obj(Obj.Var, "$tmp" + i, Tab.intType);
            temp[i].setAdr(localsCount + i);
            temp[i].setLevel(1);
        }

        currentMethod = mDecl.obj;
        currentMethod.setAdr(Code.pc);

        Code.put(Code.enter);
        Code.put(paramsCount);
        Code.put(localsCount + TEMP_SLOTS);
    }

    public void visit(VMethodDecl mDecl) {
        if (mDecl.getName().equals("main")) {
            mainPc = Code.pc;
        }
        paramsCount = mDecl.obj.getLevel();
        localsCount = mDecl.obj.getLocalSymbols().size() - paramsCount;

        for (int i = 0; i < TEMP_SLOTS; i++) {
            temp[i] = new Obj(Obj.Var, "$tmp" + i, Tab.intType);
            temp[i].setAdr(localsCount + i);
            temp[i].setLevel(1);
        }

        currentMethod = mDecl.obj;
        currentMethod.setAdr(Code.pc);

        Code.put(Code.enter);
        Code.put(paramsCount);
        Code.put(localsCount + TEMP_SLOTS);
    }

    public void visit(MethodDecl mDecl) {
        Code.put(Code.exit);
        Code.put(Code.return_);
        currentMethod = null;
    }

    //STATEMENTS
    public void visit(PrintStatement printStatement) {

        int kind = printStatement.getExpr().struct.getKind();

        if (kind == Struct.Int || kind == Struct.Bool) {
            Code.loadConst(0);
            Code.put(Code.print);
        } else if (kind == Struct.Char) {
            Code.loadConst(1);
            Code.put(Code.bprint);
        }
    }

    public void visit(ReturnValueStatement returnStatement) {
        Code.put(Code.exit);
        Code.put(Code.return_);
        currentMethod = null;
    }

    public void visit(ReturnStatement returnStatement) {
        Code.put(Code.exit);
        Code.put(Code.return_);
        currentMethod = null;
    }

    public void visit(NumConstPrintStatement printStatement) {

        // DON'T push the width - it's already pushed by visit(NumConst)!

        int kind = printStatement.getExpr().struct.getKind();

        if (kind == Struct.Int || kind == Struct.Bool) {
            Code.put(Code.print);
        } else if (printStatement.getExpr().struct.getKind() == Struct.Char) {
            Code.put(Code.bprint);
        }
    }

    public void visit(ReadStatement readStatement) {
        Obj obj = readStatement.getDesignator().obj;
        if (obj.getType().getKind() == Struct.Char) {
            Code.put(Code.bread);
            Code.store(obj);

            Code.put(Code.bread);       // Read the newline \r
            Code.put(Code.pop);         //discard it
            Code.put(Code.bread);       //read \n
            Code.put(Code.pop);         //discard it
        } else {
            Code.put(Code.read);
            Code.store(obj);
        }
    }

    //DESIGNATOR
    public void visit(BaseDesignator baseDesignator){
        if(baseDesignator.obj.getKind() != Obj.Meth){
            Code.load(baseDesignator.obj);
        }
    }

    public void visit(PeriodDesignator node) {

        PeriodElem elem = node.getPeriodElem();

        if(elem instanceof LenPeriodElem){//elem.obj is not used, we use node.obj to get the address of the array
            Code.load(elem.obj);
            Code.put(Code.arraylength);
        }

        /*
            temp[0] = val
            temp[1] = addr
            temp[2] = i
            temp[3] = arrSize

         */
        if(elem instanceof  FindAnyPeriodElem){

            Code.store(temp[0]);//val was loaded by Expr
            Code.load(elem.obj);
            Code.store(temp[1]);//address of array
            Code.loadConst(-1);
            Code.store(temp[2]);//i is -1 at the beginning
            Code.load(elem.obj);
            Code.put(Code.arraylength);
            Code.store(temp[3]);//ArrayLength

            checkAddr = Code.pc;

            Code.load(temp[2]);//load i and increment it
            Code.loadConst(1);
            Code.put(Code.add);
            Code.store(temp[2]);

            Code.load(temp[2]);//load i
            Code.load(temp[3]);//load arrayLength
            Code.putFalseJump(Code.lt, 0);//if i >= arrayLength jump to false end

            falseEndJumpFixup = Code.pc - 2;

            Code.load(temp[1]);//Load addr
            Code.load(temp[2]);//Load i
            Code.put(Code.aload);//load arr[i]//TODO FIX FOR BALOAD
            Code.load(temp[0]);//load val
            Code.putFalseJump(Code.eq, checkAddr);//jump if not equal, if equal write 1 to expr stack

            Code.loadConst(1);//load 1 to expr stack
            Code.putJump(0);//jump to end
            trueEndJumpFixup = Code.pc - 2;

            Code.fixup(falseEndJumpFixup);//if i >= arrLength put 0 on expr stack

            Code.loadConst(0);
            Code.fixup(trueEndJumpFixup);//End of execution

        }

        /*
        temp[0] = ident
        temp[1] = addr
        temp[2] = i
        temp[3] = newArrAddr
         */

        if(elem instanceof MapPeriodElem){
            Code.put(Code.astore);
            Code.putJump(checkAddr);
            Code.fixup(falseEndJumpFixup);
            Code.load(temp[3]);
        }
    }

    /*
       temp[0] = ident
       temp[1] = addr
       temp[2] = i
       temp[3] = newArrAddr
    */
    public void visit(MapIdent node){
        PeriodElem elem = (PeriodElem) node.getParent();
        MapIdent mapIdent = node;
        Struct type = elem.obj.getType().getElemType();

        temp[0] = mapIdent.obj;
        Code.load(elem.obj);
        Code.store(temp[1]);//address of array
        Code.loadConst(-1);
        Code.store(temp[2]);//i is -1 at the beginning

        Code.load(temp[1]);//load the new array length
        Code.put(Code.arraylength);

        Code.put(Code.newarray);//create the new array

        if(type == TabExtended.intType){
            Code.put(1);//word array
        }
        else if(type == TabExtended.charType){
            Code.put(0);//byte array
        }
        else if(type == TabExtended.boolType){
            Code.put(1);//word array
        }

        Code.store(temp[3]);//new array addr

        checkAddr = Code.pc;

        Code.load(temp[2]);//load i and increment it
        Code.loadConst(1);
        Code.put(Code.add);
        Code.store(temp[2]);

        Code.load(temp[2]);//load i
        Code.load(temp[1]);//load arrayLength
        Code.put(Code.arraylength);
        Code.putFalseJump(Code.lt, 0);//if i >= arrayLength jump to end

        falseEndJumpFixup = Code.pc - 2;

        Code.load(temp[1]);//Load addr
        Code.load(temp[2]);//Load i
        Code.put(Code.aload);//load arr[i]
        Code.store(temp[0]);//store to ident
        Code.load(temp[3]);//Load addr
        Code.load(temp[2]);//Load i
        //now the expr is loaded and then we jump to checkaddr
    }

    public void visit(ArrayDesignatorName node){
        Code.load(node.obj);
    }

    public void visit(ArrayDesignator node){
        //Moramo znati da li se koristi za citanje ili pisanje
        SyntaxNode parent = node.getParent();

        //Ako citamo onda imamo [arr, offset] na steku i samo saljemo aload ili baload.
        if (parent instanceof DesignatorFactor) {
            if (node.obj.getType().getKind() == Struct.Char) {
                Code.put(Code.baload);
            } else {
                Code.put(Code.aload);
            }
        }
        // Ako pisemo onda se ovo razresava u AssignopDesignatorStatement, tada je stek [arr, ind, val] i store
        // funkcija koja nam je data pozove lepo astore ili bastore.
    }

    //DesignatorStatement

    public void visit(AssignopDesignatorStatement node) {
        Code.store(node.getDesignatorStatementName().getDesignator().obj);
    }

    public void visit(ActParsDesignatorStatement node) {
        Obj meth = node.getDesignatorStatementName().getDesignator().obj;

        if (meth == Tab.ordObj || meth == Tab.chrObj) {
            Code.put(Code.pop);//No return value is needed.
            return;
        }
        if (meth == Tab.lenObj) {//No return value necessary
            Code.put(Code.arraylength);
            Code.put(Code.pop);
            return;
        }

        int offset = meth.getAdr() - Code.pc;
        Code.put(Code.call);
        Code.put2(offset);

        if (meth.getType() != Tab.noType) {
            Code.put(Code.pop);
        }
    }

    public void visit(NoActParsDesignatorStatement node) {
        Obj meth = node.getDesignatorStatementName().getDesignator().obj;

        Code.put(Code.call);
        Code.put2(meth.getAdr() - Code.pc + 1);

        if (meth.getType() != Tab.noType) {
            Code.put(Code.pop);
        }
    }


    public void visit(PlusPlusDesignatorStatement node) {
        Obj obj = node.getDesignatorStatementName().getDesignator().obj;

        Code.loadConst(1);
        Code.put(Code.add);
        Code.store(obj);
    }

    public void visit(MinusMinusDesignatorStatement node) {
        Obj obj = node.getDesignatorStatementName().getDesignator().obj;

        Code.loadConst(1);
        Code.put(Code.sub);
        Code.store(obj);
    }

    //EXPRESSIONS

    public void visit(MiddleTerm middleTerm) {

        if (middleTerm.getMulop() instanceof MulopStar) {
            Code.put(Code.mul);
        } else if (middleTerm.getMulop() instanceof MulopDiv) {
            Code.put(Code.div);
        } else if (middleTerm.getMulop() instanceof MulopMod) {
            Code.put(Code.rem);
        }
    }

    public void visit(HasTermList hasTermList) {
        if (hasTermList.getAddop() instanceof AddopMinus) {
            Code.put(Code.sub);
        } else if (hasTermList.getAddop() instanceof AddopPlus) {
            Code.put(Code.add);
        }
    }

    public void visit(MinusNoTermList termList) {
        Code.put(Code.neg);
    }


    public void visit(DesignatorFactor designatorFactor) {
        //Visited in BaseDesignator
    }

    public void visit(BoolFactor boolFactor) {
        //Value already loaded in BoolConst;
    }

    public void visit(NumFactor numFactor) {
        //Value already loaded in NumConst;
    }

    public void visit(CharFactor charFactor) {
        //Value already loaded in CharConst;
    }

    public void visit(NonEmptyFuncCallFactor node) {
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

    public void visit(EmptyFuncCallFactor node) {
        Obj meth = node.getFactorName().getDesignator().obj;

        Code.put(Code.call);
        Code.put2(meth.getAdr() - Code.pc + 1);
    }

    public void visit(NewArrayFactor node) {
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

    public void visit(NumConst numConst) {
        Code.loadConst(numConst.getVal());
    }

    public void visit(CharConst charConst) {
        Code.loadConst(charConst.getVal());
    }

    public void visit(TrueBoolConst trueBoolConst) {
        Code.loadConst(1);
    }

    public void visit(FalseBoolConst falseBoolConst) {
        Code.loadConst(0);
    }



    //CONDITION

    public void visit(ExprCondFact node){
        Code.loadConst(1);
        Code.putFalseJump(Code.eq, 0);

        getListForNode(falseJumps, node).add(Code.pc - 2);
    }

    public void visit(RelopCondFact node){
        if(node.getRelop() instanceof RelopEqualEqual){
            Code.putFalseJump(Code.eq, 0);
        }
        else if(node.getRelop() instanceof RelopNotEqual){
            Code.putFalseJump(Code.ne, 0);
        }
        else if(node.getRelop() instanceof RelopLessEqual){
            Code.putFalseJump(Code.le, 0);
        }
        else if(node.getRelop() instanceof RelopLess){
            Code.putFalseJump(Code.lt, 0);
        }
        else if(node.getRelop() instanceof RelopGreaterEqual){
            Code.putFalseJump(Code.ge, 0);
        }
        else if(node.getRelop() instanceof RelopGreater){
            Code.putFalseJump(Code.gt, 0);
        }

        getListForNode(falseJumps, node).add(Code.pc - 2);
    }

    public void visit(LastCondTerm node){
        SyntaxNode child = node.getCondFact();
        moveList(falseJumps, child, falseJumps, node);
        moveList(trueJumps, child, trueJumps, node);
    }

    public void visit(MiddleCondTerm node){

        SyntaxNode childLeft = node.getCondTerm();
        SyntaxNode childRight = node.getCondFact();

        moveList(falseJumps, childLeft, falseJumps, node);
        moveList(trueJumps, childLeft, trueJumps, node);

        moveList(falseJumps, childRight, falseJumps, node);
        moveList(trueJumps, childRight, trueJumps, node);
    }

    public void visit(OrOperation node){
        Code.putJump(0);
        getListForNode(trueJumps, node).add(Code.pc - 2);

        MiddleCondition parent = (MiddleCondition)node.getParent();
        SyntaxNode leftConditions = parent.getCondition();

        fixAllForNode(falseJumps, leftConditions);
    }

    public void visit(LastCondition node){
        SyntaxNode child = node.getCondTerm();

        moveList(falseJumps, child, falseJumps, node);
        moveList(trueJumps, child, trueJumps, node);
    }

    public void visit(MiddleCondition node){

        SyntaxNode childLeft = node.getCondition();
        SyntaxNode childMiddle = node.getOrOperation();
        SyntaxNode childRight = node.getCondTerm();

        moveList(falseJumps, childLeft, falseJumps, node);
        moveList(trueJumps, childLeft, trueJumps, node);

        moveList(falseJumps, childMiddle, falseJumps, node);
        moveList(trueJumps, childMiddle, trueJumps, node);

        moveList(falseJumps, childRight, falseJumps, node);
        moveList(trueJumps, childRight, trueJumps, node);

    }

    //IF ELSE EXPRESSION

    public void visit(HasIfBlockStart node){
        SyntaxNode condition = node.getCondition();
        SyntaxNode parent = node.getParent();
        fixAllForNode(trueJumps, condition);

        moveList(falseJumps, condition, falseJumps, parent);
        moveList(trueJumps, condition, trueJumps, parent);
    }

    public void visit(ElseKeyword node){
        Code.putJump(0);

        getListForNode(endJumps, node).add(Code.pc - 2);

        SyntaxNode parent = node.getParent();
        fixAllForNode(falseJumps, parent);
    }


    //No else keyword
    public void visit(IfStatement node){
        fixAllForNode(falseJumps, node);
    }

    public void visit(IfElseStatement node){
        SyntaxNode elseKeyword = node.getElseKeyword();
        fixAllForNode(endJumps, elseKeyword);
    }

    //TERNARY

    public void visit(TernaryExprFirst node){
        TernaryExpr parent = (TernaryExpr) node.getParent();
        SyntaxNode condition = parent.getCondition();

        fixAllForNode(trueJumps, condition);
    }

    public void visit(TernaryExprSecond node){
        Code.putJump(0);
        getListForNode(endJumps, node).add(Code.pc - 2);

        TernaryExpr parent = (TernaryExpr) node.getParent();
        SyntaxNode condition = parent.getCondition();

        fixAllForNode(falseJumps, condition);
    }

    public void visit(TernaryExpr node){
        SyntaxNode semicolon = node.getTernaryExprSecond();
        fixAllForNode(endJumps, semicolon);
    }

    //For loop

    public void visit(ForLoopBegin node){
        currentForLoop.push((ForStatement)node.getParent());//push the current for loop node to the stack
    }

    public void visit(ForLoopConditionBegin node){
        forLoopCondition.put(currentForLoop.peek(), Code.pc);//remember condition address for end of update
    }

    public void visit(HasForSecondParam node){
        Condition condition = ((HasForSecondParam) node).getCondition();
        moveList(trueJumps, condition, trueJumps, node);
        moveList(falseJumps, condition, falseJumps, node);

        Code.putJump(0);//jump from condition to body execution
        getListForNode(trueJumps, node).add(Code.pc - 2);
    }

    public void visit(NoForSecondParam node){
        Code.putJump(0);//jump from condition to body execution
        getListForNode(trueJumps, node).add(Code.pc - 2);
    }

    public void visit(ForLoopUpdateBegin node){
        forLoopUpdate.put(currentForLoop.peek(), Code.pc);//remember address for end of body execution
    }

    public void visit(ForLoopBodyBegin node){
        ForStatement parent = currentForLoop.peek();
        SyntaxNode condition = parent.getForLoopSecondParam();

        int addr = forLoopCondition.get(parent);//set the update to go to condition
        Code.putJump(addr);

        fixAllForNode(trueJumps, condition);//set the true jumps to go to body execution
    }

    public void visit(ForStatement node){
        SyntaxNode forLoopUpdateBegin = node.getForLoopUpdateBegin();
        SyntaxNode condition = node.getForLoopSecondParam();

        int addr = forLoopUpdate.get(node);
        Code.putJump(addr);


        fixAllForNode(falseJumps, condition);//set all the false jumps to exit
        fixAllForNode(forLoopBreaks, node);//Set the exit for all the break statements

        currentForLoop.pop();
    }

    public void visit(BreakStatement node){
        Code.putJump(0);
        getListForNode(forLoopBreaks, currentForLoop.peek()).add(Code.pc - 2);
    }

    public void visit(ContinueStatement node){
        int addr = forLoopUpdate.get(currentForLoop.peek());
        Code.putJump(addr);
    }
}
