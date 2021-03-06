package com.ihtsdo.snomed.service.manifest.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ihtsdo.snomed.exception.ProgrammingException;
import com.ihtsdo.snomed.model.OntologyVersion;
import com.ihtsdo.snomed.service.manifest.model.BaseRefsetCollection;
import com.ihtsdo.snomed.service.manifest.model.Manifest;
import com.ihtsdo.snomed.service.manifest.model.ManifestFileFactory;
import com.ihtsdo.snomed.service.manifest.model.ManifestFolder;
import com.ihtsdo.snomed.service.manifest.model.MimetypeProperties;
import com.ihtsdo.snomed.service.manifest.model.RefsetCollectionFactory;

@Named
public class FileSystemParser {
    private static final Logger LOG = LoggerFactory.getLogger( FileSystemParser.class );
    
    @Inject
    private RefsetCollectionParser refsetCollectionParser;
    
    @Inject
    private RefsetCollectionFactory refsetCollectionFactory;
    
    @Inject
    ManifestFileFactory manifestFileFactory;
        
    @Inject
    private MimetypeProperties mimetypeProperties;    
    
    public FileSystemParser() {
    }

    //@Transactional
    public Manifest parse(File root, OntologyVersion ontologyVersion, EntityManager em){
        Manifest manifest = new Manifest(root);
        parseFolder(manifest, ontologyVersion, em);
        return manifest;
    }
    
    public FileSystemParser setParsemode(RefsetCollectionParser.Mode mode){
        refsetCollectionParser.setParseMode(mode);
        return this;
    }
    
//    public void buildCache(EntityManager em){
//        refsetCollectionParser.buildCache(em);
//    }
    
    //@Transactional
    private ManifestFolder parseFolder(ManifestFolder parent, OntologyVersion o, EntityManager em){
        LOG.info("Parsing folder '{}'", parent.getName());
        for (File child : parent.getFile().listFiles()){
            if (child.isDirectory()){
                parent.addManifestFolder(parseFolder(new ManifestFolder(child), o, em));
            }else if (mimetypeProperties.isRefsetCollectionFile(child.getName())){
                LOG.info("Parsing refset file '{}'", child.getName());                
                try (FileInputStream iStream = new FileInputStream(child)){
                    BaseRefsetCollection refsetCollection = refsetCollectionFactory.getRefsetCollection(mimetypeProperties.getMimetype(child.getName()));
                    refsetCollectionParser.parse(iStream, o, refsetCollection, em);
                    parent.addManifestFile(manifestFileFactory.createManifestFile(child, refsetCollection));
                } catch (FileNotFoundException e) {
                    throw new ProgrammingException("File system parser broke on parsing " + parent.getName(), e);
                } catch (IOException e) {
                    throw new ProgrammingException("Unable to read file " + child.getName(), e);
                }                
            }else{
                LOG.info("Adding file '{}'", child.getName());
                parent.addManifestFile(manifestFileFactory.createManifestFile(child));
            }
        }
        return parent;
    }
}
