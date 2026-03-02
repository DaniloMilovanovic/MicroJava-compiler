package rs.ac.bg.etf.pp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import java_cup.runtime.Symbol;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rs.ac.bg.etf.pp1.util.Log4JUtils;

//Lexer test
public class MJLexerTest {

	static {
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
		Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
	}
	
	public static void main(String[] args) throws IOException {
		Logger log = Logger.getLogger(MJLexerTest.class);
		Reader br = null;
		try {
			
			File sourceCode = new File("test/test301.mj");	
			log.info("Compiling source file: " + sourceCode.getAbsolutePath());
			
			br = new BufferedReader(new FileReader(sourceCode));
			
			Yylex lexer = new Yylex(br);
			Symbol currToken = null;
			while ((currToken = lexer.next_token()).sym != sym.EOF) {
				if (currToken != null && currToken.value != null) {
					String tokenName = getSymbolName(currToken.sym);
					log.info(tokenName + " " + currToken.toString() + " " + currToken.value.toString());
				}
			}
			log.info("Finished compiling source file" + sourceCode.getAbsolutePath());
		} 
		finally {
			if (br != null) try { br.close(); } catch (IOException e1) { log.error(e1.getMessage(), e1); }
		}
	}
	
	private static String getSymbolName(int symbol) {
		// Use reflection to get the field name from sym class
		java.lang.reflect.Field[] fields = sym.class.getDeclaredFields();
		for (java.lang.reflect.Field field : fields) {
			try {
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && 
					field.getType() == int.class) {
					int value = field.getInt(null);
					if (value == symbol) {
						return field.getName();
					}
				}
			} catch (IllegalAccessException e) {
				// Ignore
			}
		}
		return "UNKNOWN_" + symbol;
	}
	
}
