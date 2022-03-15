package com.ara.genesys;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.json.JSONObject;
import org.json.JSONTokener;
import picocli.CommandLine;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@CommandLine.Command
public class ParseTables implements Runnable {

    final String VERSION = "1.0";

    @CommandLine.Option(
            names = {"-f","--filename"},
            required = true,
            description = "Provide BDS template file as input.\nValid files from BSD are in format 'datasets_*.tpl"
    )
    private String filename;

    @CommandLine.Option(
            names = {"-r","--show-results"},
            description = "Shows source type and table/views discovered in BSD files"
    )
    private boolean showResults;

    @CommandLine.Option(
            names = {"-q","--show-query"},
            description = "Shows the SQL query parsed from the BDS .tpl file"
    )
    private boolean showQuery;

    @CommandLine.Option(
            names = "--no-messages",
            description = "Suppress output messages"
    )
    private boolean moSystemMessages;

    //@CommandLine.Command(name = "version")
    private void version() {
        System.out.println(String.format("BSD Table Parser %s",VERSION));
    }

    private void msg(String s) {
        if (!moSystemMessages) {
            System.out.println(s);
        }
    }

    private JSONObject readJSON(String filename) {
        InputStream is = null;
        JSONObject result = null;
        try {
            msg("Searching for BDS file: " + filename);
            is = new FileInputStream(filename);
            JSONTokener tokener = new JSONTokener(is);
            result = new JSONObject(tokener);
            is.close();
        } catch (FileNotFoundException e) {
            msg(String.format("ERROR: %s not found!",filename));
        } catch (IOException e) {
            msg(String.format("ERROR: %s",e.getMessage()));
        }
        return result;
    }

    private void readBDSTemplate() {
        String query = "";
        String source = "";
        try {
            JSONObject object = readJSON(filename).getJSONObject("datasets");
            object = object.getJSONObject(object.keys().next());
            source = object.getString("source");
            if (showResults) msg(String.format("Source: %s", source));
            query = object.getJSONObject("primary_statements").getString("statement");
            if (showQuery) msg(query);
            parseQuery(source, query);
        } catch (NullPointerException npe) {
            
        }
    }

    private void parseQuery(String source, String query) {
        try {
            Statement stmt = CCJSqlParserUtil.parse(query);
            Select selectStatement = (Select) stmt;
            TablesNamesFinder tables = new TablesNamesFinder();
            List<String> tablesList = tables.getTableList(selectStatement);
            for (String tname : tablesList) {
                System.out.println(String.format("%s\t%s",source,tname.toUpperCase()));
            }
        } catch (JSQLParserException e) {
            msg(String.format("ERROR: No tables found in %s query!",source));
        }
    }

    @Override
    public void run() {
        readBDSTemplate();
    }

    public static void main(String [] args) {
        int exitCode = new CommandLine(new ParseTables()).execute(args);
        System.exit(exitCode);
    }
}


///TODO:
//* Fix npe if no source file is found!
//* Add 'version'  (use @Command)
//* Process multiple files?

