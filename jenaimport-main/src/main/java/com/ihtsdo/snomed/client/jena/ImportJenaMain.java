package com.ihtsdo.snomed.client.jena;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.ihtsdo.snomed.model.Ontology;
import com.ihtsdo.snomed.service.parser.HibernateParser;
import com.ihtsdo.snomed.service.parser.HibernateParserFactory;

public class ImportJenaMain {
    private static final Logger LOG = LoggerFactory.getLogger( ImportJenaMain.class );
        
    private EntityManagerFactory emf  = null;
    private EntityManager em          = null;

    private void initDb(Properties properties){
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put("hibernate.dialect", properties.getProperty(ImportJenaCliParser.HIBERNATE_DIALECT_KEY));
        overrides.put("javax.persistence.jdbc.driver", properties.getProperty(ImportJenaCliParser.DRIVER_KEY));
        overrides.put("javax.persistence.jdbc.url", properties.getProperty(ImportJenaCliParser.URL_KEY));
        overrides.put("javax.persistence.jdbc.user", properties.getProperty(ImportJenaCliParser.USER_KEY));
        overrides.put("javax.persistence.jdbc.password", properties.getProperty(ImportJenaCliParser.PASSWORD_KEY));
        overrides.put("hibernate.hbm2ddl.auto", "update");
        
        LOG.info("Connecting to database " + properties.getProperty(ImportJenaCliParser.URL_KEY));
        emf = Persistence.createEntityManagerFactory(HibernateParser.ENTITY_MANAGER_NAME_FROM_PERSISTENCE_XML, overrides);
        em = emf.createEntityManager();
        em.getTransaction().begin();
    }

    public void closeDb(){
        LOG.info("Closing database");
        em.getTransaction().commit();
        em.close();
        emf.close();
    }

    public static void main(String[] args) throws IOException, ParseException{
        Stopwatch overAllstopwatch = new Stopwatch().start();
        ImportJenaCliParser cli = new ImportJenaCliParser();
        cli.parse(args, new ImportJenaMain()); 
        overAllstopwatch.stop();
        LOG.info("Overall program completion in " + overAllstopwatch.elapsed(TimeUnit.SECONDS) + " seconds");
    }    
    
    protected void runProgram(File conceptFile, File triplesFile, File descriptionFile, Properties properties,
            HibernateParserFactory.Parser parser, String name) throws IOException
    {
        try{
            initDb(properties);
            HibernateParser hibParser = HibernateParserFactory.getParser(parser);
            Ontology o = null;
            if (descriptionFile != null){
                o = hibParser.populateDbWithDescriptions(
                        name, 
                        new FileInputStream(conceptFile), 
                        new FileInputStream(triplesFile), 
                        new FileInputStream(descriptionFile), 
                        em);
            } else if (conceptFile != null){
                o = hibParser.populateDb(
                        name, 
                        new FileInputStream(conceptFile), 
                        new FileInputStream(triplesFile), 
                        em);                        
            } else {
                o = hibParser.populateDbFromStatementsOnly(
                        name, 
                        new FileInputStream(triplesFile), 
                        new FileInputStream(triplesFile), 
                        em);
            } 

            

        }finally{
            closeDb();
        }
    }
}

