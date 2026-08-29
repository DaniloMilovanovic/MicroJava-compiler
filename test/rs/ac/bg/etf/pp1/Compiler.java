package rs.ac.bg.etf.pp1;

import java.io.*;

import java_cup.runtime.Symbol;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.util.Log4JUtils;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;

public class Compiler{

	static{
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
		Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
	}

	public static void tsdump(){
		TabExtended.dump();
	}

	public static void main(String[] args) throws Exception{

		Logger log = Logger.getLogger(Compiler.class);

		if(args.length < 2){
			System.err.println("Usage: Compiler <input.mj> <output.obj>");
			return;
		}

		Reader br = null;
		try{
			File sourceCode = new File(args[0]);
			File objFile = new File(args[1]);

			log.info("Compiling source file: " + sourceCode.getAbsolutePath());

			br = new BufferedReader(new FileReader(sourceCode));
			Yylex lexer = new Yylex(br);

			TabExtended.init();

			MJParser p = new MJParser(lexer);
			Symbol s = p.parse();

			Program prog = (Program)(s.value);

			log.info(prog.toString(""));
			log.info("===================================");

			SemanticAnalyzer v = new SemanticAnalyzer();
			prog.traverseBottomUp(v);

			log.info(" Print count calls = " + v.printCallCount);
			log.info(" Deklarisanih promenljivih ima = " + v.varDeclCount);
			log.info("===================================");

			tsdump();

			if(!p.errorDetected && v.passed()){
				log.info("Parsiranje uspesno zavrseno!");

				if(objFile.exists()) objFile.delete();

				CodeGenerator codeGenerator = new CodeGenerator();
				prog.traverseBottomUp(codeGenerator);

				Code.mainPc = codeGenerator.getMainPc();
				Code.write(new FileOutputStream(objFile));
				log.info("Generisanje koda uspesno zavrseno!");
			}
			else{
				log.error("Parsiranje NIJE uspesno zavrseno!");
			}
		}
		finally{
			if(br != null) try{
				br.close();
			}
			catch(IOException e1){
				log.error(e1.getMessage(), e1);
			}
		}
	}
}