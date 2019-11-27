package gov.va.research.ir.model;

public class FieldValue {
    public final String qualifier;
    public final String term;
   // public final BoolOp boolOp;
    public FieldValue(final String term) {
        this.term = term;
   //     this.boolOp = boolOp;
        this.qualifier = null;
    }
    public FieldValue(final String term, final String qualifier) {
        this.term = term;
   //     this.boolOp = boolOp;
        this.qualifier = qualifier;
    }
}
