package com.ihtsdo.snomed.canonical;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ihtsdo.snomed.canonical.model.Concept;
import com.ihtsdo.snomed.canonical.model.Ontology;
import com.ihtsdo.snomed.canonical.model.RelationshipStatement;

public class HibernateDatabaseImporterTest {

    private static final String DEFAULT_ONTOLOGY_NAME = "Test";
    private static HibernateDbImporter importer;
    private static Main main;

    private static final String TEST_CONCEPTS = "test_concepts.txt";
    private static final String TEST_CONCEPTS_WITH_PARSE_ERROR = "test_concepts_with_parse_error.txt";
    private static final String TEST_RELATIONSHIPS_LONG_FORM = "test_relationships_longform.txt";
    private static final String TEST_RELATIONSHIPS_LONG_FORM_WITH_PARSE_ERROR = "test_relationships_longform_with_parse_error.txt";

    private static final String TEST_RELATIONSHIPS_SHORT_FORM = "test_relationships_shortform.txt";
    private static final String TEST_RELATIONSHIPS_SHORT_FORM_WITH_PARSE_ERROR = "test_relationships_shortform_with_parse_error.txt";
    
    
    private static final String TEST_IS_KIND_OF_RELATIONSHIPS = "test_is_kind_of_relationships.txt";
    private static final String TEST_IS_KIND_OF_CONCEPTS = "test_is_kind_of_concepts.txt";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        importer = new HibernateDbImporter();
        main = new Main();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        main.initDb(null);
       // main.em.getTransaction().begin();
    }

    @After
    public void tearDown() throws Exception {
        main.closeDb();
    }

    @Test
    public void shouldPopulateConcepts() throws IOException {
        Ontology o = importer.createOntology(main.em, DEFAULT_ONTOLOGY_NAME);
        importer.populateConcepts(ClassLoader.getSystemResourceAsStream(TEST_CONCEPTS), main.em, o);
    }

    @Test
    public void shouldPopulateLongFormRelationships() throws IOException {
        Ontology o = importer.createOntology(main.em, DEFAULT_ONTOLOGY_NAME);
        importer.populateConcepts(ClassLoader.getSystemResourceAsStream(TEST_CONCEPTS), main.em, o);
        importer.populateLongFormRelationships(ClassLoader.getSystemResourceAsStream(TEST_RELATIONSHIPS_LONG_FORM), main.em, o);
    }
    
    @Test
    public void shouldPopulateShortFormRelationships() throws IOException {
        Ontology o = importer.createOntology(main.em, DEFAULT_ONTOLOGY_NAME);
        importer.populateConcepts(ClassLoader.getSystemResourceAsStream(TEST_CONCEPTS), main.em, o);
        importer.populateShortFormRelationships(ClassLoader.getSystemResourceAsStream(TEST_RELATIONSHIPS_SHORT_FORM), main.em, o);
    } 
    
    

    @Test
    public void dbShouldHave5RelationshipsAfterPopulation() throws IOException{
        Ontology o = importer.createOntology(main.em, DEFAULT_ONTOLOGY_NAME);
        importer.populateConcepts(ClassLoader.getSystemResourceAsStream(TEST_CONCEPTS), main.em, o);
        importer.populateLongFormRelationships(ClassLoader.getSystemResourceAsStream(TEST_RELATIONSHIPS_LONG_FORM), main.em, o);

        CriteriaBuilder criteriaBuilder = main.em.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        criteriaQuery.select(criteriaBuilder.count(criteriaQuery.from(RelationshipStatement.class)));

        long result = main.em.createQuery(criteriaQuery).getSingleResult();
        assertEquals(5, result);
    }

    @Test
    public void dbShouldHave7ConceptsAfterPopulation() throws IOException{
        Ontology o = importer.createOntology(main.em, DEFAULT_ONTOLOGY_NAME);
        importer.populateConcepts(ClassLoader.getSystemResourceAsStream(TEST_CONCEPTS), main.em, o);

        CriteriaBuilder criteriaBuilder = main.em.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        criteriaQuery.select(criteriaBuilder.count(criteriaQuery.from(Concept.class)));

        long result = main.em.createQuery(criteriaQuery).getSingleResult();
        assertEquals(7, result);
    }

    @Test
    public void dbShouldStoreAllDataPointsForRelationship() throws IOException{
        Ontology o = importer.createOntology(main.em, DEFAULT_ONTOLOGY_NAME);
        importer.populateConcepts(ClassLoader.getSystemResourceAsStream(TEST_CONCEPTS), main.em, o);
        importer.populateLongFormRelationships(ClassLoader.getSystemResourceAsStream(TEST_RELATIONSHIPS_LONG_FORM), main.em, o);

        TypedQuery<RelationshipStatement> query = main.em.createQuery(
                "SELECT r FROM RelationshipStatement r where r.ontology.id=1 AND r.serialisedId=" + 100000028, 
                RelationshipStatement.class);
        
        RelationshipStatement r = query.getSingleResult();        

        assertNotNull(r);
        assertNotNull(r.getSubject());
        assertNotNull(r.getObject());
        assertEquals (100000028l, r.getSerialisedId());
        assertEquals (280844000, r.getSubject().getSerialisedId());
        assertEquals (116680003, r.getRelationshipType(), 116680003);
        assertEquals (71737002, r.getObject().getSerialisedId());
        assertEquals (0, r.getCharacteristicType());
        assertEquals (0, r.getRefinability());
        assertEquals (0, r.getGroup());
    }

    @Test
    public void dbShouldStoreAllDataPointsForConcept() throws IOException{
        Ontology o = importer.createOntology(main.em, DEFAULT_ONTOLOGY_NAME);
        importer.populateConcepts(ClassLoader.getSystemResourceAsStream(TEST_CONCEPTS), main.em, o);
        
        TypedQuery<Concept> query = main.em.createQuery("SELECT c FROM Concept c where c.ontology.id=1 AND c.serialisedId=" + 280844000, Concept.class);
        Concept c = query.getSingleResult();

        assertNotNull(c);
        assertEquals (280844000, c.getSerialisedId());
        assertEquals (0, c.getStatus());
        assertEquals ("Entire body of seventh thoracic vertebra (body structure)", c.getFullySpecifiedName());
        assertEquals ("Xa1Y9", c.getCtv3id());
        assertEquals ("T-11875", c.getSnomedId());
        assertEquals (true, c.isPrimitive());
        assertEquals (1, c.getOntology().getId());
    }

    @Test
    public void shouldPopulateSubjectOfRelationshipStatementBidirectionalField() throws IOException{
        importer.populateDbFromLongForm(DEFAULT_ONTOLOGY_NAME, ClassLoader.getSystemResourceAsStream(TEST_CONCEPTS),
                ClassLoader.getSystemResourceAsStream(TEST_RELATIONSHIPS_LONG_FORM), main.em);

        Query query = main.em.createQuery("SELECT r FROM RelationshipStatement r");

        @SuppressWarnings("unchecked")
        List<RelationshipStatement> statements = (List<RelationshipStatement>) query.getResultList();

        Iterator<RelationshipStatement> stIt = statements.iterator();
        while (stIt.hasNext()){
            RelationshipStatement statement = stIt.next();
            assertTrue(statement.getSubject().getSubjectOfRelationShipStatements().contains(statement));
        }
    }

    @Test
    public void shouldPopulateKindOfAndParentOfBidirectionalFieldsForIsARelationships() throws IOException{
        importer.populateDbFromLongForm(DEFAULT_ONTOLOGY_NAME, ClassLoader.getSystemResourceAsStream(TEST_IS_KIND_OF_CONCEPTS),
                ClassLoader.getSystemResourceAsStream(TEST_IS_KIND_OF_RELATIONSHIPS), main.em);

        TypedQuery<RelationshipStatement> query = main.em.createQuery("SELECT r FROM RelationshipStatement r WHERE r.ontology.id=1", RelationshipStatement.class);
        List<RelationshipStatement> statements = (List<RelationshipStatement>) query.getResultList();

        RelationshipStatement r1000 = null;
        RelationshipStatement r2000 = null;
        RelationshipStatement r3000 = null;
        Iterator<RelationshipStatement> rIt = statements.iterator();
        while (rIt.hasNext()){
            RelationshipStatement r = rIt.next();
            if (r.getSerialisedId() == 1000){
                r1000 = r;
            }
            if (r.getSerialisedId() == 2000){
                r2000 = r;
            }
            if (r.getSerialisedId() == 3000){
                r3000 = r;
            }
        }
        assertEquals(1, r1000.getSubject().getSerialisedId());
        assertEquals(2, r2000.getSubject().getSerialisedId());
        assertEquals(3, r3000.getSubject().getSerialisedId());
        assertTrue(r1000.getSubject().getKindOfs().contains(r2000.getSubject()));
        assertTrue(r2000.getSubject().getParentOf().contains(r1000.getSubject()));
    }

    @Test
    public void shouldSkipRowAndContinueDbPopulationAfterParseError() throws IOException{
        Ontology o = importer.createOntology(main.em, DEFAULT_ONTOLOGY_NAME);
        importer.populateConcepts(ClassLoader.getSystemResourceAsStream(TEST_CONCEPTS_WITH_PARSE_ERROR), main.em, o);
        importer.populateLongFormRelationships(ClassLoader.getSystemResourceAsStream(TEST_RELATIONSHIPS_LONG_FORM_WITH_PARSE_ERROR), main.em, o);

        CriteriaBuilder criteriaBuilder = main.em.getCriteriaBuilder();
        CriteriaQuery<Long> conceptCriteriaQuery = criteriaBuilder.createQuery(Long.class);
        conceptCriteriaQuery.select(criteriaBuilder.count(conceptCriteriaQuery.from(Concept.class)));
        long conceptResult = main.em.createQuery(conceptCriteriaQuery).getSingleResult();

        CriteriaQuery<Long> relationshipCriteriaQuery = criteriaBuilder.createQuery(Long.class);
        relationshipCriteriaQuery.select(criteriaBuilder.count(relationshipCriteriaQuery.from(RelationshipStatement.class)));
        long relationshipResult = main.em.createQuery(relationshipCriteriaQuery).getSingleResult();

        assertEquals(9, conceptResult);
        assertEquals(9, relationshipResult);
    }

    @Test
    public void shouldCreateOntologyDatabaseEntryWithAllDataPointsOnImport() throws IOException{
        Ontology ontology = importer.populateDbFromLongForm(DEFAULT_ONTOLOGY_NAME, ClassLoader.getSystemResourceAsStream(TEST_CONCEPTS),
                ClassLoader.getSystemResourceAsStream(TEST_RELATIONSHIPS_LONG_FORM), main.em);

        assertNotNull(ontology);
        assertEquals(5, ontology.getRelationshipStatements().size());
        assertEquals(1, ontology.getId());
        RelationshipStatement r = new RelationshipStatement();
        r.setSerialisedId((100000028));
        assertTrue(ontology.getRelationshipStatements().contains(r));
    }
    
    //Add test for set operations for all data imported using hibernate
    //to test that the persistence layer is working
    
    //Add test for loading multiple ontologies
    //make sure the serialiseids for concepts don't get confused
    //and that relationships point to the correct concept ids
    
    //add test for createIsRelationship routine
}
