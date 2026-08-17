package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.*;

public class TabExtended extends Tab {
	
	public static final Struct boolType = new Struct(Struct.Bool);
	public static final Struct intArrayType = new Struct(Struct.Array, intType);
	public static final Struct charArrayType = new Struct(Struct.Array, charType);
	public static final Struct boolArrayType = new Struct(Struct.Array, boolType);
	
	public static void init() {
		Tab.init();
		
		currentScope.addToLocals(new Obj(Obj.Type, "bool", boolType));
	}
	
	
	public static void dump() {
        DumpSymbolTableVisitorExtended stv = new DumpSymbolTableVisitorExtended();
        System.out.println("=====================SYMBOL TABLE DUMP=========================");
        for (Scope s = currentScope; s != null; s = s.getOuter()) {
            s.accept(stv);
        }
        System.out.println(stv.getOutput());
    }
	
}
