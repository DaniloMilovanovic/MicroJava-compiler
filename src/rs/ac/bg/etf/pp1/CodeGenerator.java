package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class CodeGenerator extends VisitorAdaptor {

    private int mainPc = -1;
    private Obj currentMethod = null;
    private int paramsCount = 0;
    private int localsCount = 0;

    public int getMainPc() {
        return mainPc;
    }

    // ==================== PROGRAM ====================

    @Override
    public void visit(Program program) {
        // Count global variables for data size
        int globalVarCount = 0;
        for (Obj obj : program.getProgName().obj.getLocalSymbols()) {
            if (obj.getKind() == Obj.Var) {
                globalVarCount++;
            }
        }
        Code.dataSize = globalVarCount;
    }

    // ==================== METHOD DECLARATION ====================

    @Override
    public void visit(TMethodDecl methodDecl) {
        currentMethod = methodDecl.obj;
        paramsCount = methodDecl.obj.getLevel();
        localsCount = methodDecl.obj.getLocalSymbols().size();

        methodDecl.obj.setAdr(Code.pc);

        if (methodDecl.getName().equals("main")) {
            mainPc = Code.pc;
        }

        // Emit enter with params and locals
        Code.put(Code.enter);
        Code.put(paramsCount);
        Code.put(localsCount);
    }

    @Override
    public void visit(VMethodDecl methodDecl) {
        currentMethod = methodDecl.obj;
        paramsCount = methodDecl.obj.getLevel();
        localsCount = methodDecl.obj.getLocalSymbols().size();

        methodDecl.obj.setAdr(Code.pc);

        if (methodDecl.getName().equals("main")) {
            mainPc = Code.pc;
        }

        // Emit enter with params and locals
        Code.put(Code.enter);
        Code.put(paramsCount);
        Code.put(localsCount);
    }

    @Override
    public void visit(MethodDecl methodDecl) {
        // Called AFTER body is visited (bottom-up traversal)
        // Emit exit and return
        Code.put(Code.exit);
        Code.put(Code.return_);

        currentMethod = null;
    }

    // ==================== RETURN ====================

    @Override
    public void visit(ReturnStatement returnStatement) {
        Code.put(Code.exit);
        Code.put(Code.return_);
    }

    @Override
    public void visit(ReturnValueStatement returnValueStatement) {
        // Expr value is on stack - leave it there as return value
        Code.put(Code.exit);
        Code.put(Code.return_);
    }

    // ==================== CONSTANTS ====================

    @Override
    public void visit(NumConst numConst) {
        Code.loadConst(numConst.getVal());
    }

    @Override
    public void visit(CharConst charConst) {
        Code.loadConst(charConst.getVal());
    }

    @Override
    public void visit(TrueBoolConst boolConst) {
        Code.loadConst(1);
    }

    @Override
    public void visit(FalseBoolConst boolConst) {
        Code.loadConst(0);
    }

    // ==================== DESIGNATORS ====================

    @Override
    public void visit(BaseDesignator designator) {
        // Load variable or constant
        Code.load(designator.obj);
    }

    @Override
    public void visit(DesignatorFactor designatorFactor) {
        // Designator is already visited (value on stack)
        // Nothing to do here
    }

    // ==================== ARITHMETIC ====================

    @Override
    public void visit(MiddleTerm term) {
        // Stack: term_value, factor_value
        if (term.getMulop() instanceof MulopStar) {
            Code.put(Code.mul);
        }
        else if (term.getMulop() instanceof MulopDiv) {
            Code.put(Code.div);
        }
        else if (term.getMulop() instanceof MulopMod) {
            Code.put(Code.rem);
        }
    }

    @Override
    public void visit(HasTermList termList) {
        // Stack: termList_value, term_value
        if (termList.getAddop() instanceof AddopPlus) {
            Code.put(Code.add);
        }
        else if (termList.getAddop() instanceof AddopMinus) {
            Code.put(Code.sub);
        }
    }

    @Override
    public void visit(MinusExpr minusExpr) {
        // Stack: value
        Code.put(Code.neg);
    }

    // ==================== ASSIGNMENT ====================

    @Override
    public void visit(AssignopDesignatorStatement assign) {
        // Expr value is on stack
        // Store to designator
        Designator des = assign.getDesignatorStatementName().getDesignator();

        if (des instanceof BaseDesignator) {
            BaseDesignator baseDes = (BaseDesignator) des;
            Code.store(baseDes.obj);
        }
        // Array assignment will be added later
    }

    // ==================== INCREMENT/DECREMENT ====================

    @Override
    public void visit(PlusPlusDesignatorStatement plusPlus) {
        Designator des = plusPlus.getDesignatorStatementName().getDesignator();

        if (des instanceof BaseDesignator) {
            BaseDesignator baseDes = (BaseDesignator) des;
            Code.load(baseDes.obj);
            Code.loadConst(1);
            Code.put(Code.add);
            Code.store(baseDes.obj);
        }
    }

    @Override
    public void visit(MinusMinusDesignatorStatement minusMinus) {
        Designator des = minusMinus.getDesignatorStatementName().getDesignator();

        if (des instanceof BaseDesignator) {
            BaseDesignator baseDes = (BaseDesignator) des;
            Code.load(baseDes.obj);
            Code.loadConst(1);
            Code.put(Code.sub);
            Code.store(baseDes.obj);
        }
    }

    // ==================== PRINT ====================

    @Override
    public void visit(PrintStatement printStatement) {
        // Expr value is on stack
        Struct type = printStatement.getExpr().struct;

        if (type == TabExtended.charType) {
            Code.loadConst(1);  // width for char
            Code.put(Code.bprint);
        }
        else {
            Code.loadConst(5);  // width for int/bool
            Code.put(Code.print);
        }
    }

    // ==================== PASS-THROUGH NODES ====================
    // These don't need code generation - traversal handles them

    @Override
    public void visit(NoTExpr expr) {
        // No code - NoTernaryExpr is already visited
    }

    @Override
    public void visit(NoMinusExpr expr) {
        // No code - TermList is already visited
    }

    @Override
    public void visit(NoTermList termList) {
        // No code - Term is already visited
    }

    @Override
    public void visit(LastTerm term) {
        // No code - Factor is already visited
    }

    @Override
    public void visit(NumFactor numFactor) {
        // No code - NumConst is already visited
    }

    @Override
    public void visit(CharFactor charFactor) {
        // No code - CharConst is already visited
    }

    @Override
    public void visit(BoolFactor boolFactor) {
        // No code - BoolConst is already visited
    }

    @Override
    public void visit(ExprFactor exprFactor) {
        // No code - Expr is already visited
    }

    @Override
    public void visit(DesignatorStatementName name) {
        // No code - Designator is already visited
    }

    @Override
    public void visit(Assignop assignop) {
        // No code - '=' is just a token
    }

    @Override
    public void visit(AddopPlus addop) {
        // No code - handled in HasTermList
    }

    @Override
    public void visit(AddopMinus addop) {
        // No code - handled in HasTermList
    }

    @Override
    public void visit(MulopStar mulop) {
        // No code - handled in MiddleTerm
    }

    @Override
    public void visit(MulopDiv mulop) {
        // No code - handled in MiddleTerm
    }

    @Override
    public void visit(MulopMod mulop) {
        // No code - handled in MiddleTerm
    }

    @Override
    public void visit(Type type) {
        // No code - resolved during semantic analysis
    }
}