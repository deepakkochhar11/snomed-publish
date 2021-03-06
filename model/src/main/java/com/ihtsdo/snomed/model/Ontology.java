package com.ihtsdo.snomed.model;

import java.sql.Date;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

@Table(
    name = "Ontology", 
    uniqueConstraints = {
            @UniqueConstraint(columnNames = {"publicId"})
    })
@Entity
public class Ontology {

    @Id 
    @GeneratedValue(strategy=GenerationType.IDENTITY)    
    private Long id;
    
    @NotNull
    private String label;
    
    @OneToMany(cascade=CascadeType.ALL, fetch = FetchType.EAGER, mappedBy="ontology")
    private Set<OntologyFlavour> flavours = new HashSet<>();
    
    @NotNull
    private String publicId;
    
    @NotNull
    private Date creationTime;
    
    @NotNull
    private Date modificationTime;
    
    @Version
    private long version = 0;

    @Override
    public String toString(){
        return com.google.common.base.Objects.toStringHelper(this)
                .add("id", getId())
                .add("publicId", getPublicId())
                .add("label", getLabel())
                .add("flavours", getFlavours())
                .toString();
    }
    
    @Override
    public int hashCode(){
        return java.util.Objects.hash(
                //getId(),
                getPublicId(),
                getLabel(),
                getFlavours());
    }
    
    @Override
    public boolean equals(Object o){
        if (o instanceof Ontology){
            Ontology ontology = (Ontology) o;
            if (
                //Objects.equals(ontology.getId(), getId()) &&
                Objects.equals(ontology.getPublicId(), getPublicId()) &&
                Objects.equals(ontology.getLabel(), getLabel()) &&
                Objects.equals(ontology.getFlavours(), getFlavours())){
                return true;
            }
        }
        return false;
    }    
    
    @PreUpdate
    public void preUpdate() {
        modificationTime = new Date(Calendar.getInstance().getTime().getTime());
    }
    
    @PrePersist
    public void prePersist() {
        Date now = new Date(Calendar.getInstance().getTime().getTime());
        creationTime = now;
        modificationTime = now;
    }
    
    public Set<OntologyFlavour> addFlavour(OntologyFlavour flavour) {
        getFlavours().add(flavour);
        flavour.setOntology(this);
        return flavours;
    }

    
    public Set<OntologyFlavour> getFlavours() {
        return flavours;
    }

    public void setFlavours(Set<OntologyFlavour> flavours) {
        this.flavours = flavours;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public Date getModificationTime() {
        return modificationTime;
    }

    public void setModificationTime(Date modificationTime) {
        this.modificationTime = modificationTime;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
    
    
    
}
