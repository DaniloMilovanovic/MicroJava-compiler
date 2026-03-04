package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.concepts.Struct;
import rs.etf.pp1.symboltable.visitors.DumpSymbolTableVisitor;

public class DumpSymbolTableVisitorExtended extends DumpSymbolTableVisitor {
    
    public void visitStructNode(Struct structToVisit) {
        if (structToVisit.getKind() == Struct.Bool) {
            output.append("bool");
            return;
        }
        
        super.visitStructNode(structToVisit);
    }
}