package com.ihtsdo.snomed.canonical.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.common.primitives.Longs;
import com.ihtsdo.snomed.canonical.service.InvalidInputException;

public class Group {
    
    protected final Set<StatementWrapperForAttributeCompare> statements = new HashSet<StatementWrapperForAttributeCompare>();
    private Concept groupForConcept;
    
    public Group(){}
    
    public Group(Statement statement){
        groupForConcept = statement.getSubject();
        this.statements.add(new StatementWrapperForAttributeCompare(statement));
    }
    
    public Group(Collection<Statement> statements) throws InvalidInputException{
        if (groupForConcept == null) groupForConcept = statements.iterator().next().getSubject();
        for (Statement s : statements){
            if (!groupForConcept.equals(s.getSubject())){
                throw new InvalidInputException("Attempting to add statement " + s.toString() + " to group allready determined to be for concept " + groupForConcept.toString());
            }
            this.statements.add(new StatementWrapperForAttributeCompare(s));
        }
    }

    public void addStatement(Statement statement){
        this.statements.add(new StatementWrapperForAttributeCompare(statement));
    }
    
    @Override
    public String toString(){
        String value = "Group: {";
        for (StatementWrapperForAttributeCompare statement : statements){
            value += statement.getStatement().shortToString() + ", ";
        }
        value += "}";
        return value;
    }
    
    @Override
    public int hashCode(){
        return statements.size();
    }
    
    @Override
    public boolean equals (Object o){
        if (o instanceof Group){
            Group g = (Group) o;
            if (g.statements.size() != this.statements.size()){
                return false;
            }
            
            if (this.statements.equals(g.statements)){
                return true;
            }
        }
        return false;
    }
    
    protected static class StatementWrapperForAttributeCompare{
        Statement statement;
        
        public StatementWrapperForAttributeCompare(Statement statement){
            this.statement = statement;
        }
        
        @Override
        public boolean equals(Object o){
            if (o instanceof StatementWrapperForAttributeCompare){
                StatementWrapperForAttributeCompare r = (StatementWrapperForAttributeCompare) o;            
                if (r.statement.getPredicate().equals(this.statement.getPredicate()) &&
                    r.statement.getObject().equals(this.statement.getObject()))
                {
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public int hashCode(){
            return Longs.hashCode(statement.getPredicate().getSerialisedId());
        }
        
        public String toString(){
            return "wrapped: " + statement.toString();
        }
        
        public Statement getStatement(){
            return statement;
        }
    }    
}
