package com.ihtsdo.snomed.service.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ejb.HibernateEntityManager;
import org.hibernate.jdbc.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.ihtsdo.snomed.exception.InvalidInputException;
import com.ihtsdo.snomed.model.Concept;
import com.ihtsdo.snomed.model.OntologyVersion;
import com.ihtsdo.snomed.model.OntologyVersion.Source;

public class ChildParentHibernateParser extends HibernateParser{
    private static final Logger LOG = LoggerFactory.getLogger( ChildParentHibernateParser.class );

    ChildParentHibernateParser(){}
    
    protected Source getSource(){
        return Source.CHILD_PARENT;
    }
    
    protected void populateStatements(final InputStream stream, EntityManager em, 
            final OntologyVersion ontologyVersion) throws IOException 
    {
        LOG.info("Populating statements");
        final Map<Long, Long> map = createConceptSerialisedIdToDatabaseIdMap(ontologyVersion, em);
        HibernateEntityManager hem = em.unwrap(HibernateEntityManager.class);
        Session session = ((Session) hem.getDelegate()).getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
        session.doWork(new Work() {
            public void execute(Connection connection) throws SQLException {
                PreparedStatement ps = connection.prepareStatement("INSERT INTO Statement (serialisedid, subject_id, predicate_id, object_id, groupId, characteristicTypeIdentifier, refinability, effectiveTime, active, ontologyVersion_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                try (@SuppressWarnings("resource")BufferedReader br = new BufferedReader(new InputStreamReader(stream))){
                    int currentLine = 1;
                    String line = null;
                    try {
                        line = br.readLine();
                        long isAPredicateDatabaseId = map.get(Concept.IS_KIND_OF_RELATIONSHIP_TYPE_ID);
                        while (line != null) {
                            currentLine++;
                            if (line.isEmpty()){
                                line = br.readLine();
                                continue;
                            }
                            Iterable<String> split = Splitter.on('\t').split(line);
                            Iterator<String> splitIt = split.iterator();
                            try {
                                ps.setLong(1, com.ihtsdo.snomed.model.Statement.SERIALISED_ID_NOT_DEFINED);
                                {//subject
                                    String conceptSerialisedId = splitIt.next();
                                    Long conceptId = map.get(new Long(conceptSerialisedId));
                                    if (conceptId == null){
                                        throw new InvalidInputException("Concept " + conceptId + " not found for statement");
                                    }
                                    ps.setLong(2, conceptId);
                                }
                                ps.setLong(3, isAPredicateDatabaseId);
                                {//object
                                    String conceptSerialisedId = splitIt.next();
                                    Long conceptId = map.get(new Long(conceptSerialisedId));
                                    if (conceptId == null){
                                        throw new InvalidInputException("Concept " + conceptId + " not found for statement");
                                    }
                                    ps.setLong(4, conceptId);
                                }
                                ps.setInt(5, DEFAULT_STATEMENT_GROUP);
                                ps.setInt(6, DEFAULT_STATEMENT_CHARACTERISTIC_TYPE_IDENTIFIER);
                                ps.setInt(7,  DEFAULT_STATEMENT_REFINABILITY);
                                ps.setInt(8, DEFAULT_STATEMENT_EFFECTIVE_TIME);
                                ps.setBoolean(9, DEFAULT_STATEMENT_ACTIVE);
                                ps.setLong(10, ontologyVersion.getId());
                                ps.addBatch();
                            } catch (NumberFormatException e) {
                                LOG.error("Unable to parse line number " + currentLine + ". Line was [" + line + "]. Message is [" + e.getMessage() + "]. Skipping entry and continuing", e);
                                if (parseMode.equals(Mode.STRICT)){throw new InvalidInputException(e);}
                            } catch (IllegalArgumentException e){
                                LOG.error("Unable to parse line number " + currentLine + ". Line was [" + line + "]. Message is [" + e.getMessage() + "]. Skipping entry and continuing", e);
                                if (parseMode.equals(Mode.STRICT)){throw new InvalidInputException(e);}
                            }
                            line = br.readLine();
                        }
                        ps.executeBatch();
                        LOG.info("Populated " + (currentLine - 1) + " statements");
                    }finally {
                        br.close();
                    }
                } catch (IOException e1) {
                    LOG.error("Unable to read from the input stream. Bailing out. Message is: " + e1.getMessage(), e1);
                } 
            }
        });
        tx.commit();
    }

    @Override
    protected void populateConceptsFromStatements(final InputStream stream,
            EntityManager em, final OntologyVersion ontologyVersion) throws IOException 
    {
        LOG.info("Populating concepts");
        HibernateEntityManager hem = em.unwrap(HibernateEntityManager.class);
        Session session = ((Session) hem.getDelegate()).getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
        session.doWork(new Work() {
            public void execute(Connection connection) throws SQLException {
                PreparedStatement ps = connection.prepareStatement("INSERT INTO Concept (serialisedId, ontologyVersion_id, primitive, statusId, active, effectiveTime, version) VALUES (?, ?, ?, ?, ?, ?, ?)");
                Set<Concept> concepts = new HashSet<Concept>();
                try (@SuppressWarnings("resource") BufferedReader br = new BufferedReader(new InputStreamReader(stream))){
                    int currentLine = 1;
                    String line = br.readLine();
                    while (line != null) {
                        currentLine++;
                        if (line.isEmpty()){
                            line = br.readLine();
                            continue;
                        }
                        Iterable<String> split = Splitter.on('\t').split(line);
                        Iterator<String> splitIt = split.iterator();
                        try {                            
                            concepts.add(new Concept(Long.parseLong(splitIt.next()))); //child
                            concepts.add(new Concept(Long.parseLong(splitIt.next()))); //parent
                        } catch (NumberFormatException e) {
                            LOG.error("Unable to parse line number " + currentLine + ". Line was [" + line + "]. Message is [" + e.getMessage() + "]. Skipping entry and continuing", e);
                            if (parseMode.equals(Mode.STRICT)){throw new InvalidInputException(e);}
                        } catch (IllegalArgumentException e){
                            LOG.error("Unable to parse line number " + currentLine + ". Line was [" + line + "]. Message is [" + e.getMessage() + "]. Skipping entry and continuing", e);
                            if (parseMode.equals(Mode.STRICT)){throw new InvalidInputException(e);}
                        }
                        line = br.readLine();
                    }
                    
                    for (Concept c : concepts){
                        ps.setLong(1, c.getSerialisedId());
                        ps.setLong(2, ontologyVersion.getId());
                        ps.setBoolean(3, DEFAULT_CONCEPT_PRIMITIVE);
                        ps.setInt(4, DEFAULT_CONCEPT_STATUS_ID);
                        ps.setBoolean(5, DEFAULT_CONCEPT_ACTIVE);
                        ps.setInt(6, DEFAULT_CONCEPT_EFFECTIVE_TIME);
                        ps.setInt(7, DEFAULT_VERSION);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                    LOG.info("Populated " + concepts.size() + " concepts");
                } catch (IOException e1) {
                    LOG.error("Unable to read from the input stream. Bailing out. Message is: " + e1.getMessage(), e1);
                } 
            }
        });
        tx.commit();
        setKindOfPredicate(em, ontologyVersion);
    }

    @Override
    protected void populateConcepts(InputStream stream, EntityManager em,
            final OntologyVersion ontologyVersion) throws IOException {
        throw new UnsupportedOperationException("Not supported");
    }    
    
    @Override
    protected void populateDescriptions(InputStream stream, EntityManager em,
            OntologyVersion ontologyVersion) throws IOException {
        throw new UnsupportedOperationException("Not supported");        
    }
    
    @Override
    protected void populateConceptsFromStatementsAndDescriptions(final InputStream statementsStream, 
            final InputStream descriptionsStream, EntityManager em, final OntologyVersion ontologyVersion) throws IOException{
        throw new UnsupportedOperationException("Not supported");
    }    
}
