package com.ihtsdo.snomed.canonical;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

public class CliParser {

    public void parse(String[] args, Main callback) throws IOException, ParseException{

        //Create a parser using Commons CLI
        CommandLineParser parser = new BasicParser( );
        Options options = new Options( );
        options.addOption("h", "help", false, "Print this usage information");
        options.addOption("t", "triples", true, "Triples input file");
        options.addOption("c", "concepts", true, "Concepts input file");
        options.addOption("o", "output", true, "Destination file");
        options.addOption("d", "database", true, "Database location");

        String helpString = "-t <triples input file> -c <concepts input file> -o <output file>. Optionaly -d <database location>. Try -h for more help";
        
        // Parse the program arguments
        CommandLine commandLine = null;
        try {
            commandLine = parser.parse(options, args);
        } catch (UnrecognizedOptionException e) {
            System.out.println("Unrecognised option. Usage is " + helpString);
            System.exit(1);
        }

        // Set the appropriate variables based on supplied options
        String output = commandLine.getOptionValue('o');
        String concepts = commandLine.getOptionValue('c');
        String triples = commandLine.getOptionValue('t');
        String db = commandLine.getOptionValue('d');

        testInputs(helpString, commandLine, output, concepts, triples, db);
        callback.runProgram(concepts, triples, output, db);
    }
    
    private static void testInputs(String helpString, CommandLine commandLine,
            String output, String concepts, String triples, String db) {
        if (commandLine.hasOption('h')) {
            System.out.println("-h, --help\t\tPrint this help menu\n" +
                    "-t. --triples\t\tFile containing all the relationships that you want to process, aka 'Relationships_Core'\n" +
                    "-c, --concepts\t\tFile containing all the concepts referenced in the relationships file, aka 'Concepts_Core'\n" +
                    "-o, --output\t\tDestination file to write the canonical output results to\n" +
                    "-d, --database\t\tOptional. Specify location of database file. If not specified, \n\t\t\tdefaults to an in-memory database (minium 2Gb of heap space required)");
            System.exit(0);
        }
        
        if ((output == null) || (concepts == null) || (triples == null) ||
                (output.isEmpty()) || (concepts.isEmpty()) || triples.isEmpty()){
            System.out.println("Invalid parameter configuration. Usage is: " + helpString);
            System.exit(-1);
        }
        
        if (!new File(concepts).isFile()){
            System.out.println("Unable to locate concepts input file '" + concepts + "'");
            System.exit(-1);
        }
        
        if (!new File(concepts).isFile()){
            System.out.println("Unable to locate triples input file '" + triples + "'");
            System.exit(-1);
        }
        
        try {
            new FileOutputStream(new File(output));
        } catch (IOException e) {
            System.out.println("Unable to write to output file '" + output +"'. Check your permissions and path.");
            System.exit(-1);
        }
        
        if ((db != null) && (!db.isEmpty())){
            try {
                new FileOutputStream(new File(db));
            } catch (IOException e) {
                System.out.println("Unable to write to database file '" + db +"'. Check your permissions and path.");
                System.exit(-1);
            }
        }
    }    
}
