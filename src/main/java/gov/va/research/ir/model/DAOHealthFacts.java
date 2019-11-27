package gov.va.research.ir.model;

import gov.va.research.ir.SearchUtils;
import gov.va.research.ir.model.SearchResult.Encounter;
import gov.va.research.ir.model.SearchResult.Patient;
import gov.va.research.ir.model.SearchResult.PDiag;
import gov.va.research.ir.model.SearchResult.PMedication;
import gov.va.research.ir.model.SearchResult.PLResult;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import javax.sql.DataSource;



import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.lang.*;
import javax.sql.DataSource;
import javax.swing.*;

import gov.va.vinci.nlp.qeUtils.domain.TermWeight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;

import com.vividsolutions.jts.geom.Coordinate;

import static java.lang.Math.abs;


public class DAOHealthFacts implements DAODataSource {

    private static final String UUID = java.util.UUID.randomUUID().toString()
            .replace('-', '_');
    private static final String UUID2 = java.util.UUID.randomUUID().toString()
            .replace('-', '_');
    private static final String UUID3 = java.util.UUID.randomUUID().toString()
            .replace('-', '_');

    static final String TEMP_PATIENT_TABLE = "tmpA_pat_" + UUID;
    static final String TEMP_DOCUMENT_TABLE = "tmp_doc_" + UUID;

    static final String TEMP_ENCOUNTER_TABLE= "tmp_enc" + UUID2;


    static final String TEMP_E_YEAR_TABLE= "tmp_enc" + UUID3;

    //static final String TEMP_DIAGNOSIS_TABLE= "temp_enc" + UUID3;
  //


    private DataSource dataSource;
    private String displayName;

    protected List<Statement> activeStatements = new ArrayList<Statement>();
    private boolean explainPlans = false;

    public DAOHealthFacts() {
    }

    public DAOHealthFacts(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setup() throws SQLException {
        executeStatements(new String[]{
                "drop table if exists " + TEMP_PATIENT_TABLE + ";",
                "drop table if exists " + TEMP_ENCOUNTER_TABLE + ";",
                "drop table if exists " + TEMP_DOCUMENT_TABLE + ";"




        });
    }
    protected Map<Integer, Integer> findIntegerDistribution2 (
            final String selectSql) throws SQLException {
        Connection conn = null;
        Map<Integer, Integer> intDistMap = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            PreparedStatement ps = null;
            try {
                ps = conn
                        .prepareStatement(selectSql,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
                activeStatements.add(ps);
                ResultSet rs = null;
                try {
                    long startTime = System.currentTimeMillis();

                    rs = ps.executeQuery();
                    intDistMap = new HashMap<Integer, Integer>();
                    while (rs.next()) {
                        Integer integer = Integer.valueOf(rs.getInt(1));
                        Integer count = Integer.valueOf(rs.getInt(2));
                        intDistMap.put(integer, count);
                    }

                    long endTime = System.currentTimeMillis();
                    Long elapsed = Long.valueOf(endTime - startTime);
                    System.out.println("querying time of "+ selectSql + " "+ elapsed+" milliseconds. \n");
                } finally {
                    if (rs != null) {
                        rs.close();
                    }
                }
            } finally {
                if (ps != null) {
                    ps.close();
                    activeStatements.remove(ps);
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }
        return intDistMap;
    }





    private String buildQueryMatchingPatients(final List<SearchTerm> searchTerms) {
        String db = "hf_jul_2016";
        Map<Field, List<FieldValue>> fieldValueMap = buildFieldValueMap(searchTerms);
        final Set<Field> queryFields = fieldValueMap.keySet();
        //hf_Jul_2016.hf_f_diagnosis_" + fieldValueMap.get(Field.ENCOUNTER_YEAR).get(0).term




// Seyed
        StringBuilder fromSB = new StringBuilder("from "  ) ;
        if (queryFields.contains(Field.ENCOUNTER_YEAR)) {

            List<FieldValue> queryTs = fieldValueMap.get(Field.ENCOUNTER_YEAR);
            for (FieldValue queryTm : queryTs) {


 //               if  (queryTm.term.startsWith(">")  || (queryTm.term.startsWith("<"))  || (queryTm.term.contains("-"))  )  {

                    StringBuilder where = new StringBuilder();
                    boolean o = addWhereTerm(true, fieldValueMap.get(Field.ENCOUNTER_YEAR), "enc_year", where);


                    fromSB.append("  hf_jul_2016.hf_f_encounter_bigtab e join (select enc_year from hf_jul_2016.hf_for_query_purpose " +  where.toString() + "  ) qp on e.enc_year = qp.enc_year ");

 //               }
//                else {
//
//
//                    fromSB.append(db +".hf_f_encounter_" + queryTm.term + " e "  );
//
//                }
            }

        }

        else {

         fromSB.append("  hf_jul_2016.hf_f_encounter_bigtab e inner join (select enc_year from hf_jul_2016.hf_for_query_purpose where enc_year = 2016 ) qp on e.enc_year = qp.enc_year ");
//            fromSB.append(db +".hf_f_encounter_2016 e ");

        }





        // StringBuilder fromSB = new StringBuilder( "from " + db + ".hf_f_encounter e");
        StringBuilder whereSB = new StringBuilder();
        boolean firstCriterion = true;

        boolean include_columns_encounter= false;
        if (queryFields.contains(Field.AGE) || queryFields.contains(Field.CARESETTING_SID))
            include_columns_encounter = true;

        boolean include_columns_non_encounter=false;
        if (queryFields.contains(Field.GENDER) || queryFields.contains(Field.DIAGNOSIS)||queryFields.contains(Field.ICD9CODE)  ||queryFields.contains(Field.MEDICATION) )
            include_columns_non_encounter= true;

        String patientSQL = "";



        patientSQL += " CREATE  TABLE " + TEMP_PATIENT_TABLE + " AS SELECT e.patient_id, e.encounter_id, e.discharge_disposition_id, e.age_in_years, e.admitted_dt_tm, e.discharged_dt_tm , e.gender, e.race  " + //

   //             patientSQL +=  "  SELECT e.patient_id, e.encounter_id, e.discharge_disposition_id, e.age_in_years, e.admitted_dt_tm, e.discharged_dt_tm  into temporary table " + TEMP_PATIENT_TABLE +

                        " " +
                fromSB.toString()
                + " ";


        if (include_columns_encounter && ! include_columns_non_encounter){
//            if (queryFields.contains(Field.CARESETTING_SID)){
//                firstCriterion = addWhereTerm(firstCriterion, fieldValueMap.get(Field.CARESETTING_SID), "discharge_caresetting_id", whereSB);
//                patientSQL = patientSQL + " " + whereSB.toString();
//                if (queryFields.contains(Field.AGE)){
//                    firstCriterion = addWhereTerm(firstCriterion, fieldValueMap.get(Field.AGE), "age_in_years", whereSB);
//                    //patientSQL = patientSQL + " and age_in_years=" + fieldValueMap.get(Field.AGE).get(0).term ;
//                    patientSQL = patientSQL + " "+ whereSB.toString();
//                }
//            }
//            else
            if (queryFields.contains(Field.AGE)){
                firstCriterion = addWhereTerm(firstCriterion, fieldValueMap.get(Field.AGE), "age_in_years", whereSB);
                patientSQL = patientSQL + " "+ whereSB.toString();
            }
        }

        if (include_columns_non_encounter){
            if (queryFields.contains(Field.GENDER)) {
            //    patientSQL = patientSQL + " inner join hf_jul_2016.hf_d_patient p on p.patient_id= e.patient_id and p.gender= '"+fieldValueMap.get(Field.GENDER).get(0).term+"'";
                patientSQL = patientSQL + "  and e.gender::citext = '"+fieldValueMap.get(Field.GENDER).get(0).term+"'";

            }
            if (queryFields.contains(Field.DIAGNOSIS)){

                String diagnosisNames_string = fieldValueMap.get(Field.DIAGNOSIS).get(0).term;

                List myls = fieldValueMap.get(Field.DIAGNOSIS);

                StringBuilder mystring = new StringBuilder();

                String pre = "";
                for ( int i=0 ; i <myls.size(); i++) {
                    String myst = fieldValueMap.get(Field.DIAGNOSIS).get(i).term;
                    mystring.append(pre);
                    pre = "@";
                    mystring.append(myst);
           }

                 String ss= mystring.toString();
                String[] diagnosisNames = ss.trim().split("@", myls.size());

                if (diagnosisNames_string.endsWith("*")){

                    patientSQL = patientSQL + " inner join " + db + ".hf_f_diagnosis " +  " df on df.encounter_id=e.encounter_id" +
                            " inner join hf_jul_2016.hf_d_diagnosis dd on dd.diagnosis_id = df.diagnosis_id  and dd.diagnosis_description::citext like '" + fieldValueMap.get(Field.DIAGNOSIS).get(0).term.substring(0,diagnosisNames_string.length()-1) + "%'";
                }

                else {
                    if (diagnosisNames.length > 1) {
 //                   if (str.length > 1) {
                        String tmp = null;
                        for (String diagnosisName : diagnosisNames) {
                            if (tmp == null) {
                                tmp = "'" + diagnosisName + "'";
                            } else
                                tmp = tmp + ",'" + diagnosisName + "'";
                        }
                        patientSQL = patientSQL + " inner join " + db + ".hf_f_diagnosis" +  " df on df.encounter_id=e.encounter_id" +
                                " inner join hf_jul_2016.hf_d_diagnosis dd on dd.diagnosis_id = df.diagnosis_id and dd.diagnosis_description::citext in (" + tmp + ")";
                    } else
                        patientSQL = patientSQL + " inner join " + db + ".hf_f_diagnosis df on df.encounter_id=e.encounter_id" +
                                " inner join hf_jul_2016.hf_d_diagnosis dd on dd.diagnosis_id = df.diagnosis_id " + "   and  " +
                                "dd.diagnosis_description::citext = '" + fieldValueMap.get(Field.DIAGNOSIS).get(0).term + "'";


                }
            }



            if (queryFields.contains(Field.ICD9CODE)){

                List mylsICD = fieldValueMap.get(Field.ICD9CODE);

                StringBuilder mystringICD = new StringBuilder();

                String pre = "";
                for ( int i=0 ; i <mylsICD.size(); i++) {
                    String myst = fieldValueMap.get(Field.ICD9CODE).get(i).term;
                    mystringICD.append(pre);
                    pre = "@";
                    mystringICD.append(myst);
                }

                String ss= mystringICD.toString();
                String[] icdNames = ss.trim().split("@", mylsICD.size());




                String icdNames_string = fieldValueMap.get(Field.ICD9CODE).get(0).term;

                if (icdNames_string.endsWith("*")){
                    patientSQL = patientSQL + " inner join " + db + ".hf_f_diagnosis " +  " df on df.encounter_id=e.encounter_id" +
                            " inner join hf_jul_2016.hf_d_diagnosis dd on dd.diagnosis_id = df.diagnosis_id and dd.diagnosis_code like '" + fieldValueMap.get(Field.ICD9CODE).get(0).term.substring(0,icdNames_string.length()-1) + "%'";
                }

                else {
                    if (icdNames.length > 1) {
                        String tmp = null;
                        for (String icdName : icdNames) {
                            if (tmp == null) {
                                tmp = "'" + icdName + "'";
                            } else
                                tmp = tmp + ",'" + icdName + "'";
                        }
                        patientSQL = patientSQL + " inner join " +db + ".hf_f_diagnosis"  + " df on df.encounter_id=e.encounter_id" +
                                " inner join hf_jul_2016.hf_d_diagnosis dd on dd.diagnosis_id = df.diagnosis_id and dd.diagnosis_code in (" + tmp + ")";
                    } else
                        patientSQL = patientSQL + " inner join " + db + ".hf_f_diagnosis "  + " df on df.encounter_id=e.encounter_id" +
                                " inner join hf_jul_2016.hf_d_diagnosis dd on dd.diagnosis_id = df.diagnosis_id and dd.diagnosis_code='" + fieldValueMap.get(Field.ICD9CODE).get(0).term + "'";
                }
            }



            if (queryFields.contains(Field.MEDICATION)){

                List mylsMed = fieldValueMap.get(Field.MEDICATION);

                StringBuilder mystringMed = new StringBuilder();

                String pre = "";
                for ( int i=0 ; i <mylsMed.size(); i++) {
                    String mystMed = fieldValueMap.get(Field.MEDICATION).get(i).term;
                    mystringMed.append(pre);
                    pre = "@";
                    mystringMed.append(mystMed);
                }

                String Medss= mystringMed.toString();
                String[] MedNames = Medss.trim().split("@", mylsMed.size());

                String drugNames_string = fieldValueMap.get(Field.MEDICATION).get(0).term;

                if (drugNames_string.endsWith("*")){
                    patientSQL = patientSQL + " inner join " + db + ".hf_f_medication" +  " mf on mf.encounter_id=e.encounter_id" +
                            " inner join " + db + ".hf_d_medication md on md.medication_id = mf.medication_id and md.generic_name::citext like '" + fieldValueMap.get(Field.MEDICATION).get(0).term.substring(0,drugNames_string.length()-1) + "%'";

                }
                else {

                    if (MedNames.length > 1) {
                        String tmp = null;
                        for (String drugName : MedNames) {
                            if (tmp == null) {
                                tmp = "'" + drugName + "'";
                            } else
                                tmp = tmp + ",'" + drugName + "'";
                        }
                        patientSQL = patientSQL + " inner join " + db + ".hf_f_medication mf on mf.encounter_id=e.encounter_id" +
                                " inner join hf_jul_2016.hf_d_medication md on md.medication_id = mf.medication_id and md.generic_name::citext in (" + tmp + ")";
                    } else
                        patientSQL = patientSQL + " inner join " + db + ".hf_f_medication mf on mf.encounter_id=e.encounter_id" +
                                " inner join hf_jul_2016.hf_d_medication md on md.medication_id = mf.medication_id and md.generic_name::citext ='" + fieldValueMap.get(Field.MEDICATION).get(0).term + "'";
                }
            }



            if (queryFields.contains(Field.SURGICAL_PROCEDURE)){

                String surgical_string = fieldValueMap.get(Field.SURGICAL_PROCEDURE).get(0).term;
                if (surgical_string.endsWith("*")){
                    patientSQL = patientSQL + " inner join " + db + ".hf_f_surgical_procedure" +  " fs on fs.encounter_id=e.encounter_id" +
                            " inner join hf_jul_2016.hf_d_surgical_procedure ds on ds.surgical_procedure_id = fs.surgical_procedure_id and ds.surgical_procedure_desc::citext like '" + fieldValueMap.get(Field.SURGICAL_PROCEDURE).get(0).term.substring(0,surgical_string.length()-1) + "%'";

                }
                else {
                    String[] surgicalNames = surgical_string.trim().split("\\|");
                    if (surgicalNames.length > 1) {
                        String tmp = null;
                        for (String surgicalName : surgicalNames) {
                            if (tmp == null) {
                                tmp = "'" + surgicalName + "'";
                            } else
                                tmp = tmp + ",'" + surgicalName + "'";
                        }
                        patientSQL = patientSQL + " inner join " + db + ".hf_f_surgical_procedure" +  " fs on fs.encounter_id=e.encounter_id" +
                                " inner join hf_jul_2016.hf_d_surgical_procedure ds on ds.surgical_procedure_id = fs.surgical_procedure_id and ds.surgical_procedure_desc::citext in (" + tmp + ")";
                    } else
                        patientSQL = patientSQL + " inner join " + db + ".hf_f_surgical_procedure" +  " fs on fs.encounter_id=e.encounter_id" +
                                " inner join hf_jul_2016.hf_d_surgical_procedure ds on ds.surgical_procedure_id = fs.surgical_procedure_id and ds.surgical_procedure_desc::citext ='" + fieldValueMap.get(Field.SURGICAL_PROCEDURE).get(0).term + "'";
                }
            }


            if (queryFields.contains(Field.CLINICAL_EVENT)){

                String clinical_string = fieldValueMap.get(Field.CLINICAL_EVENT).get(0).term;
                if (clinical_string.endsWith("*")){
                    patientSQL = patientSQL + " inner join hf_jul_2016.hf_f_clinical_event" +  " fc on fc.encounter_id=e.encounter_id" +
                            " inner join hf_jul_2016.hf_d_event_code dc on dc.event_code_id = fc.event_code_id and dc.event_code_desc::citext like '" + fieldValueMap.get(Field.CLINICAL_EVENT).get(0).term.substring(0,clinical_string.length()-1) + "%'";

                }
                else {
                    String[] clinicalNames = clinical_string.trim().split("\\|");
                    if (clinicalNames.length > 1) {
                        String tmp = null;
                        for (String surgicalName : clinicalNames) {
                            if (tmp == null) {
                                tmp = "'" + surgicalName + "'";
                            } else
                                tmp = tmp + ",'" + surgicalName + "'";
                        }
                        patientSQL = patientSQL + "  inner join hf_jul_2016.hf_f_clinical_event" +  " fc on fc.encounter_id=e.encounter_id" +
                                " inner join hf_jul_2016.hf_d_event_code dc on dc.event_code_id = fc.event_code_id and dc.event_code_desc::citext in (" + tmp + ")";
                    } else
                        patientSQL = patientSQL + " inner join hf_jul_2016.hf_f_clinical_event" +  " fc on fc.encounter_id=e.encounter_id" +
                                " inner join hf_jul_2016.hf_d_event_code dc on dc.event_code_id = fc.event_code_id and dc.event_code_desc::citext ='" + fieldValueMap.get(Field.CLINICAL_EVENT).get(0).term + "'";
                }
            }




            // Seyed
            if (queryFields.contains(Field.CPT)){
                String diagnosisNames_string = fieldValueMap.get(Field.CPT).get(0).term;
                if (diagnosisNames_string.endsWith("*")){
                    patientSQL = patientSQL + " inner join " + db + ".hf_f_procedure" +  " fp on fp.encounter_id=e.encounter_id" +
                            " inner join hf_jul_2016.hf_d_procedure dp on dp.procedure_id = fp.procedure_id and dp.procedure_code like '" + fieldValueMap.get(Field.CPT).get(0).term.substring(0,diagnosisNames_string.length()-1) + "%'";

                }
                else {
                    String[] diagnosisNames = diagnosisNames_string.trim().split("\\|");
                    if (diagnosisNames.length > 1) {
                        String tmp = null;
                        for (String diagnosisName : diagnosisNames) {
                            if (tmp == null) {
                                tmp = "'" + diagnosisName + "'";
                            } else
                                tmp = tmp + ",'" + diagnosisName + "'";
                        }
                        patientSQL = patientSQL + " inner join " + db + ".hf_f_procedure" +  " fp on fp.encounter_id=e.encounter_id" +
                                " inner join hf_jul_2016.hf_d_procedure dp on dp.procedure_id = fp.procedure_id and dp.procedure_code in (" + tmp + ")";
                    } else
                        patientSQL = patientSQL + " inner join " + db + ".hf_f_procedure" +  " fp on fp.encounter_id=e.encounter_id" +
                                " inner join hf_jul_2016.hf_d_procedure dp on dp.procedure_id = fp.procedure_id and dp.procedure_code='" + fieldValueMap.get(Field.CPT).get(0).term + "'";
                }
            }





			/*  //Seyed
			if (queryFields.contains(Field.LAB)){
				//patientSQL = patientSQL + " inner join " + db + ".hf_f_lab_procedure_"+year+" lf on lf.encounter_id=e.encounter_id and lf.numeric_result= "+fieldValueMap.get(Field.LAB).get(0).term +
				//		" inner join hf_jul_2016.hf_d_lab_procedure ld on ld.lab_procedure_id = lf.detail_lab_procedure_id and ld.lab_procedure_name='"+fieldValueMap.get(Field.LAB).get(0).qualifier+"'";
				String labNames_string = fieldValueMap.get(Field.LAB).get(0).qualifier;
				String[] labNames = labNames_string.trim().split("\\|");
				if (labNames.length>1){
					String tmp=null;
					for (String labName: labNames){
						if (tmp==null){
							tmp = "'"+ labName+"'";
						}
						else
							tmp=tmp+",'"+ labName+"'";
					}
					if (fieldValueMap.get(Field.LAB).get(0).term.trim().length()==0) {
						patientSQL = patientSQL + " inner join " + db + ".hf_f_lab_procedure_" + year + " lf on lf.encounter_id=e.encounter_id and lf.numeric_result= " + fieldValueMap.get(Field.LAB).get(0).term +
								" inner join hf_jul_2016.hf_d_lab_procedure ld on ld.lab_procedure_id = lf.detail_lab_procedure_id and ld.lab_procedure_name in (" + tmp + ")";
					}
					else{
						patientSQL = patientSQL + " inner join " + db + ".hf_f_lab_procedure_" + year + " lf on lf.encounter_id=e.encounter_id and lf.numeric_result " + parseLabValueTerm(fieldValueMap.get(Field.LAB).get(0).term) +
								" inner join hf_jul_2016.hf_d_lab_procedure ld on ld.lab_procedure_id = lf.detail_lab_procedure_id and ld.lab_procedure_name in (" + tmp + ")";
					}
				}
				else
				if (fieldValueMap.get(Field.LAB).get(0).term.trim().length()==0){
					patientSQL = patientSQL + " inner join " + db + ".hf_f_lab_procedure_"+year+" lf on lf.encounter_id=e.encounter_id and lf.numeric_result= "+fieldValueMap.get(Field.LAB).get(0).term +
							" inner join hf_jul_2016.hf_d_lab_procedure ld on ld.lab_procedure_id = lf.detail_lab_procedure_id and ld.lab_procedure_name='"+fieldValueMap.get(Field.LAB).get(0).qualifier+"'";
				}
				else{
					patientSQL = patientSQL + " inner join " + db + ".hf_f_lab_procedure_"+year+" lf on lf.encounter_id=e.encounter_id and lf.numeric_result "+parseLabValueTerm(fieldValueMap.get(Field.LAB).get(0).term) +
							" inner join hf_jul_2016.hf_d_lab_procedure ld on ld.lab_procedure_id = lf.detail_lab_procedure_id and ld.lab_procedure_name='"+fieldValueMap.get(Field.LAB).get(0).qualifier+"'";
				}
			}
			*/


            if (include_columns_encounter){
//                if (queryFields.contains(Field.CARESETTING_SID))
//                    patientSQL = patientSQL + " and e.discharge_caresetting_id=" + fieldValueMap.get(Field.CARESETTING_SID).get(0).term ;

                if (queryFields.contains(Field.AGE)){
                    //patientSQL = patientSQL + " and e.age_in_years=" + fieldValueMap.get(Field.AGE).get(0).term ;
                    firstCriterion = addWhereTerm(firstCriterion, fieldValueMap.get(Field.AGE), "age_in_years", whereSB);
                    patientSQL = patientSQL + " "+ whereSB.toString();
                }
            }
        }
        return patientSQL;
    }

    class PreOrder implements Iterator<Integer> {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Integer next() {
            return null;
        }
    }

    public static void main(String argv[]) {
        List<Integer>  a = new ArrayList<>();
    }

    public void prepareQueryMatchingPatients(final List<SearchTerm> searchTerms) throws Exception {
        Map<Field, List<FieldValue>> fieldValueMap = buildFieldValueMap(searchTerms);

        // Prepare
        //      String dropTempTable = "DROP TABLE IF EXISTS " + TEMP_PATIENT_TABLE;
        //     String createTempPatientTable = " CREATE TABLE " + TEMP_PATIENT_TABLE + " (patient_id bigint, encounter_id bigint, discharge_disposition_id bigint)";//, gender character, race character, age_in_years integer, encounter_id bigint  )";
        //      executeStatements(new String[]{dropTempTable,createTempPatientTable});

    }
    public void postQueryMatchingPatients(final List<SearchTerm> searchTerms) throws Exception {
        String dropIndexSQL_enc = "DROP INDEX IF EXISTS ix_TEMP_PATIENT_TABLE_encid";
        String patIndexSQL_enc = "CREATE INDEX ix_TEMP_PATIENT_TABLE_encid ON hfx.TEMP_PATIENT_TABLE (encounter_id)";

        String dropIndexSQL_pat = "DROP INDEX IF EXISTS ix_TEMP_PATIENT_TABLE_patid";
        String patIndexSQL_pat = "CREATE INDEX ix_TEMP_PATIENT_TABLE_patid ON hfx.TEMP_PATIENT_TABLE (patient_id)";


        String dropIndexSQL_en = "DROP INDEX IF EXISTS ix_"+TEMP_ENCOUNTER_TABLE+"_encid";
        //       String patIndexSQL_en = "CREATE INDEX ix_"+TEMP_ENCOUNTER_TABLE+"_encid ON " + TEMP_PATIENT_TABLE + " (encounter_id)";

        String dropIndexSQL_enp = "DROP INDEX IF EXISTS ix_"+TEMP_ENCOUNTER_TABLE+"_patid";
        //       String patIndexSQL_pac = "CREATE INDEX ix_"+TEMP_ENCOUNTER_TABLE+"_patid ON " + TEMP_ENCOUNTER_TABLE + " (patient_id)";



        //      String analyzeSQL = "ANALYZE "+ TEMP_PATIENT_TABLE;

        String[] sqls = new String[] { dropIndexSQL_enc, patIndexSQL_enc, dropIndexSQL_pat, patIndexSQL_pat,  dropIndexSQL_enc, patIndexSQL_enc, dropIndexSQL_en,dropIndexSQL_enp };
        executeStatements(sqls);

    }

    public String queryMatchingPatients(final List<SearchTerm> searchTerms)
            throws Exception {

        String[] sql=new String[3];
        sql[0] = " DROP TABLE IF EXISTS " + TEMP_PATIENT_TABLE;
        sql[1]= buildQueryMatchingPatients(searchTerms);

        sql[2] = " DROP TABLE IF EXISTS " + TEMP_ENCOUNTER_TABLE;
//
//        sql[3] = " create table  " + TEMP_ENCOUNTER_TABLE +  " AS  select e.patient_id, e.encounter_id,  e.admitted_dt_tm, e.discharged_dt_tm  from hf_jul_2016.hf_f_encounter_2016 e " +
//                " inner join "  + TEMP_PATIENT_TABLE+ " tmp on e.patient_id= tmp.patient_id ";   // all encounters of a patient


        System.out.println(sql[0]);
        System.out.println(sql[1]);



        executeStatements(sql);
        return sql[0];
    }



//    public String queryMatchingDiagnoses ()
//            throws SQLException {
//
//        String[] sql=new String[2];
//
//        sql[0] = " DROP TABLE IF EXISTS " + TEMP_DIAGNOSIS_TABLE;
//
//        sql[1]= " create table " + TEMP_DIAGNOSIS_TABLE + " AS select e_t.patient_id, e_t.encounter_id, dd.diagnosis_description ,e_t.admitted_dt_tm, e_t.discharged_dt_tm " +
//                " from " + TEMP_ENCOUNTER_TABLE + " e_t " +
//                "inner join hf_jul_2016.hf_f_diagnosis fd on e_t.encounter_id=fd.encounter_id " +
//                "inner join hf_jul_2016.hf_d_diagnosis dd on dd.diagnosis_id= fd.diagnosis_id ";
//
//
//
//
//
//
//        System.out.println(sql[0]);
//        System.out.println(sql[1]);
//
//        executeStatements(sql);
//        System.out.println( "Done");
//        return sql[0];
//    }




    //add by Huijuan 08/28/2017
    public List<String> findPopupNames(final String selectSql) throws SQLException {
        Connection conn = null;

        List<String> popupNames = new ArrayList<String>();
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            Statement namesStat = null;
            try {
                namesStat = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_READ_ONLY);
                activeStatements.add(namesStat);
                ResultSet namesRS = null;
                try {
                    //added by Huijuan 07/25/2017
                    long startTime = System.currentTimeMillis();

                    namesRS = namesStat.executeQuery(selectSql);
                    //countRS.next();
                    //count = countRS.getInt(1);
                    while (namesRS.next()) {
                        String name = namesRS.getString("generic_name");
                        int count = namesRS.getInt("count");
                        System.out.println(name + "\t" + count);
                        popupNames.add(name);
                    }

                    long endTime = System.currentTimeMillis();
                    Long elapsed = Long.valueOf(endTime - startTime);
                    System.out.println("querying time of "+ selectSql + " "+ elapsed+" milliseconds. \n");

                } finally {
                    if (namesRS != null) {
                        namesRS.close();
                    }
                }
            } finally {
                if (namesStat != null) {
                    namesStat.close();
                    activeStatements.remove(namesStat);
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }

        return popupNames;
    }



    public void executeStatements(String[] sqls) throws SQLException {
        if ( sqls == null || sqls.length == 0 ) return;
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            for (String sql : sqls) {
                if ( sql == null || sql.isEmpty() ) continue;
                Statement stmt = null;
                try {
                    stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                            ResultSet.CONCUR_READ_ONLY);
                    activeStatements.add(stmt);
                    //added by Huijuan 07/25/2017 to record the time
                    long startTime = System.currentTimeMillis();
                    System.out.println("Execute S" +sql);
                    stmt.execute(sql);
                    long endTime = System.currentTimeMillis();
                    Long elapsed = Long.valueOf(endTime - startTime);
                    System.out.println("querying time of "+ sql + " "+ elapsed+" milliseconds. \n");
                } finally {
                    System.err.println("finally statement after query");
                    if (stmt != null) {
                        stmt.close();
                        activeStatements.remove(stmt);
                    }
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }
    }

    String parseLabValueTerm(String queryTerm){
        //  String WhereTerm(final List<FieldValue> queryTerm){
        StringBuffer whereSB = new StringBuffer();
        if (queryTerm.startsWith(">")) {
            whereSB.append(" > " + queryTerm.substring(1));
        } else if (queryTerm.startsWith("<")) {
            whereSB.append(" < " + queryTerm.substring(1));
        } else {
            Matcher rangeMatcher = SearchUtils.RANGE_PATTERN.matcher(queryTerm);
            if (rangeMatcher.find()) {
                String beginExp = queryTerm.substring(0, rangeMatcher.start(1));
                String endExp = queryTerm.substring(rangeMatcher.end(1));
                whereSB.append(" BETWEEN " + beginExp + " AND " + endExp);
            } else {
                String value = SearchUtils.STAR_PATTERN.matcher(queryTerm).replaceAll("%");
                Matcher m = SearchUtils.ANDOR_PATTERN.matcher(value);
                if (m.find()) {
                    int start = 0;
                    int end = m.start();
                    String op = null;
                    String v = value.substring(start, end).trim();
                    if (v.contains("%")) {
                        op = "LIKE";
                    } else {
                        op = "=";
                    }
                    whereSB.append(" " + op + " '" + v + "' " + m.group());
                    start = m.end() + 1;
                    while (m.find()) {
                        end = m.start();
                        v = value.substring(start, end).trim();
                        //whereSB.append(" " + columnName + " ");
                        //whereSB.append(" e." + columnName + " ");
                        if (v.contains("%")) {
                            op = " LIKE ";
                        } else {
                            op = "=";
                        }
                        whereSB.append(" " + op + " '" + v + "' " + m.group());
                        start = m.end() + 1;
                    }
                    v = value.substring(start).trim();
                    //whereSB.append(" " + columnName + " ");
                    if (v.contains("%")) {
                        op = "LIKE";
                    } else {
                        op = "=";
                    }
                    whereSB.append(" " + op + " '" + v + "'");
                } else {
                    if (value.contains("%")) {
                        whereSB.append(" LIKE '" + value + "'");
                    } else {
                        //whereSB.append(" = '" + value + "'");
                        // update by Huijuan 07/20/2017
                        whereSB.append(" = " + value + "");
                    }
                }
            }
        }
        return whereSB.toString();
    }

    boolean addWhereTerm(boolean firstCriterion, final List<FieldValue> queryTerms,
                         final String columnName, final StringBuilder whereSB) {
        if (queryTerms != null && queryTerms.size() > 0) {
            String separator = null;
            if (firstCriterion) {
                separator = " WHERE ";
                //     firstCriterion = false;
            } else {
                separator = "and"; //queryTerms.get(0).boolOp.toString();  //Seyed
            }
            whereSB.append(" " + separator + " (");
            boolean firstFieldQT = true;
            for (FieldValue queryTerm : queryTerms) {
                if (firstFieldQT) {
                    firstFieldQT = false;
                } else {
                    whereSB.append(" and "); //queryTerm.boolOp.forSql()); //Seyed
                }
                whereSB.append(" " + columnName);
                //whereSB.append(" e." + columnName);
                if (queryTerm.term.startsWith(">")) {
                    whereSB.append(" > " + queryTerm.term.substring(1));
                } else if (queryTerm.term.startsWith("<")) {
                    whereSB.append(" < " + queryTerm.term.substring(1));
                } else {
                    Matcher rangeMatcher = SearchUtils.RANGE_PATTERN.matcher(queryTerm.term);
                    if (rangeMatcher.find()) {
                        String beginExp = queryTerm.term.substring(0, rangeMatcher.start(1));
                        String endExp = queryTerm.term.substring(rangeMatcher.end(1));
                        whereSB.append(" BETWEEN " + beginExp + " AND " + endExp);
                    } else {
                        String value = SearchUtils.STAR_PATTERN.matcher(queryTerm.term).replaceAll("%");
                        Matcher m = SearchUtils.ANDOR_PATTERN.matcher(value);
                        if (m.find()) {
                            int start = 0;
                            int end = m.start();
                            String op = null;
                            String v = value.substring(start, end).trim();
                            if (v.contains("%")) {
                                op = "LIKE";
                            } else {
                                op = "=";
                            }
                            whereSB.append(" " + op + " '" + v + "' " + m.group());
                            start = m.end() + 1;
                            while (m.find()) {
                                end = m.start();
                                v = value.substring(start, end).trim();
                                whereSB.append(" " + columnName + " ");
                                //whereSB.append(" e." + columnName + " ");
                                if (v.contains("%")) {
                                    op = " LIKE ";
                                } else {
                                    op = "=";
                                }
                                whereSB.append(" " + op + " '" + v + "' " + m.group());
                                start = m.end() + 1;
                            }
                            v = value.substring(start).trim();
                            whereSB.append(" " + columnName + " ");
                            if (v.contains("%")) {
                                op = "LIKE";
                            } else {
                                op = "=";
                            }
                            whereSB.append(" " + op + " '" + v + "'");
                        } else {
                            if (value.contains("%")) {
                                whereSB.append(" LIKE '" + value + "'");
                            } else {
                                //whereSB.append(" = '" + value + "'");
                                // update by Huijuan 07/20/2017
                                whereSB.append(" = " + value + "");
                            }
                        }
                    }
                }
            }
            whereSB.append(")");
            separator = " AND ";
        }
        return firstCriterion;
    }


    Map<Field, List<FieldValue>> buildFieldValueMap(final List<SearchTerm> searchTerms) {
        Map<Field, List<FieldValue>> fieldValueMap = new HashMap<Field, List<FieldValue>>(
                Field.values().length);

        for (SearchTerm st : searchTerms) {
            List<FieldValue> sl = fieldValueMap.get(st.field);
            if (sl == null) {
                sl = new ArrayList<FieldValue>();
                fieldValueMap.put(st.field, sl);
            }
            String term = st.term;
            if ( Field.ENCOUNTER_YEAR.equals(st.field) )
                term = term.replace('|','_');
            sl.add(new FieldValue(term, st.qualifier));  // , st.boolOp //Seyed

        }
        return fieldValueMap;
    }


    public Map<Integer, Integer> getAgeDistribution() throws SQLException {
        String db = "hf_jul_2016";

        return findIntegerDistribution2(
                "SELECT age_in_years, COUNT(age_in_years) "+
                        " FROM (select distinct patient_id, age_in_years from " + TEMP_PATIENT_TABLE  + " ) f GROUP BY age_in_years ORDER BY age_in_years ASC");
    }







    public Map<String, Integer> getGenderDistribution() throws SQLException {
        return findStringDistribution2(
//                "SELECT gender, COUNT(gender) "+
//                        " FROM (select distinct s.patient_id, case when gender in('Null' ,'NULL', 'Not Mapped', 'Other') then 'Unknown' " +
//                        "else gender end as gender from  "  + TEMP_PATIENT_TABLE +
//                        " s INNER JOIN hf_jul_2016.hf_d_patient p " +
//                        " ON p.patient_id = s.patient_id ) ma " +
//                        " GROUP BY gender "

                "SELECT gender, COUNT(gender) "+
                        " FROM (select distinct patient_id, case when gender = 'Unknown/Invalid' then 'Unknown' " +
                        "else gender end as gender from  "  + TEMP_PATIENT_TABLE +
                           " ) ma " +
                        " GROUP BY gender "

//                "SELECT gender, COUNT(gender) "+
//                        " from  "  + TEMP_PATIENT_TABLE +
//                        " GROUP BY gender "




        );

    }



//Seyed

    public Map<String, Integer> getRaceDistribution() throws SQLException {
        return findStringDistribution2(


//
//                "SELECT race, COUNT(race) FROM (select distinct s.patient_id, case when race in('Null' ,'NULL', 'Not Mapped', 'Other') then 'Unknown' " +
//                        "else race end as Race from  " +TEMP_PATIENT_TABLE + " s inner join hf_jul_2016.hf_d_patient p  ON p.patient_id = s.patient_id) Se group by race"

                " SELECT race, COUNT(race)  " +
                        " from  " +TEMP_PATIENT_TABLE + " group by race "



        );

    }


    public Map<String, Integer> getMedicationDistribution() throws SQLException {

        return findStringDistribution2(

                " select generic_name, count(generic_name) from hf_jul_2016.hf_d_medication dm " +
                        " inner join hf_jul_2016.hf_f_medication fm on dm.medication_id=fm.medication_id " +
                        " inner join " + TEMP_PATIENT_TABLE  +  " e on e.encounter_id=fm.encounter_id  "
                      +" group by generic_name order by count(generic_name) desc limit 25 "

 //                       " select generic_name, count from hf_jul_2016.Top_Medication "

        );
    }



        public Map<String, Integer> getDxDistribution(int topCutoff) throws SQLException {
        String db = "hf_jul_2016";


        return findStringDistribution2(

            "    select diagnosis_description, count(diagnosis_description) " +
             "   from hf_jul_2016.hf_d_diagnosis dd " +
              "  inner join hf_jul_2016.hf_f_diagnosis fd on dd.diagnosis_id=fd.diagnosis_id " +
                    "  inner join hf_jul_2016.hf_f_encounter e on e.encounter_id = fd.encounter_id " +
              "  inner join " + TEMP_PATIENT_TABLE  +  " tmp on tmp.patient_id=e.patient_id " +

              "  group by diagnosis_description  order by count(diagnosis_description) desc limit 25 "




    //            " select diagnosis_description from hf_jul_2016.hf_d_diagnosis limit 4 "

        );



    }


    public Map<String, Integer> getDeceasedDistribution() throws SQLException {

        return findStringDistribution2(
                " SELECT dischg_disp_code_desc, COUNT(dischg_disp_code_desc) FROM (select distinct patient_id, case when discharge_disposition_id in " +
                        " (11,19,20,21) " +
                        " then 'Deceased' " +
                        " else 'Alive' end as dischg_disp_code_desc from  " +TEMP_PATIENT_TABLE + " ) Se group by dischg_disp_code_desc "
        );
    }



//@Override
//    public Map<String, Integer> getRxDistribution(int topCutoff) throws SQLException {  //for the Prescription tab
//        String db = "hf_jul_2016";
//
//
//        return findStringDistribution2(
//          //      " select generic_name, count(generic_name) from "+db+".hf_d_medication_test md group by generic_name limit 25"
//
//                " select diagnosis_description, Num from hf_jul_2016.hf_d_diagnosis d inner join ( " +
//                        " select fd.diagnosis_id , count(diagnosis_id) as Num " +
//                        " from   hf_jul_2016.hf_f_diagnosis fd " +
//
//                        " inner join " + TEMP_PATIENT_TABLE +
//                        " pt on pt.encounter_id = fd.encounter_id group by fd.diagnosis_id ) H on H.diagnosis_id = d.diagnosis_id limit 19"
//
//        );
//
//    }








    public int getMatchingPatientCount() throws SQLException {
        return findCount2("select count(distinct patient_id) from " + TEMP_PATIENT_TABLE );  //"SELECT COUNT(p.encounter_id) FROM " + TEMP_PATIENT_TABLE + " p"
    }

    public int findCount2(final String selectSql) throws SQLException {
        Connection conn = null;
        int count = -1;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            Statement countStat = null;
            try {
                countStat = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_READ_ONLY);
                activeStatements.add(countStat);
                ResultSet countRS = null;
                try {
                    //added by Huijuan 07/25/2017
                    long startTime = System.currentTimeMillis();

                    countRS = countStat.executeQuery(selectSql);
                    countRS.next();
                    count = countRS.getInt(1);

                    long endTime = System.currentTimeMillis();
                    Long elapsed = Long.valueOf(endTime - startTime);
                    System.out.println("querying time of "+ selectSql + " "+ elapsed+" milliseconds. \n");

                } finally {
                    if (countRS != null) {
                        countRS.close();
                    }
                }
            } finally {
                if (countStat != null) {
                    countStat.close();
                    activeStatements.remove(countStat);
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }

        return count;
    }



    @Override
    public int getQueryTimeoutMinutes() {
        // TODO Auto-generated method stub
        return 120;
    }

    @Override
    public void setQueryTimeoutMinutes(int minutes) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public boolean hasSensitiveData() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isValid() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getMatchingDocumentCount() throws SQLException, IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    public List<CodeNameCount> findCodeCountsByPatient(final String patientId,
                                                       final String selectSql) throws SQLException {
        Connection conn = null;
        List<CodeNameCount> codeCountList = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            PreparedStatement ps = null;
            try {
                ps = conn
                        .prepareStatement(selectSql,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
                activeStatements.add(ps);
                ps.setString(1, patientId);
                codeCountList = new ArrayList<CodeNameCount>();
                ResultSet rs = null;
                try {
                    long startTime = System.currentTimeMillis();

                    rs = ps.executeQuery();
                    while (rs.next()) {
                        codeCountList.add(new CodeNameCount(rs.getString(1), rs
                                .getString(2), Integer.valueOf(rs.getInt(3))));
                    }

                    long endTime = System.currentTimeMillis();
                    Long elapsed = Long.valueOf(endTime - startTime);
                    System.out.println("querying time of "+ selectSql + " "+ elapsed+" milliseconds. \n");

                } finally {
                    if (rs != null) {
                        rs.close();
                    }
                }
            } finally {
                if (ps != null) {
                    ps.close();
                    activeStatements.remove(ps);
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }
        return codeCountList;
    }




    // 09/20/2017, by Huijuan, find the counts of the hospital_d distribution
//    public Map<Coordinate, Integer> findGeoDistribution(final String selectSql)
//            throws SQLException {
//        Connection conn = null;
//        Map<Coordinate, Integer> geoDistMap = null;
//        try {
//            conn = DataSourceUtils.getConnection(dataSource);
//            if (explainPlans) {
//                explainPlan2(selectSql, conn);
//            }
//            PreparedStatement ps = null;
//            try {
//                ps = conn
//                        .prepareStatement(selectSql,
//                                ResultSet.TYPE_FORWARD_ONLY,
//                                ResultSet.CONCUR_READ_ONLY);
//                activeStatements.add(ps);
//                ResultSet rs = null;
//                try {
//                    long startTime = System.currentTimeMillis();
//
//                    rs = ps.executeQuery();
//                    geoDistMap = new HashMap<Coordinate, Integer>();
//                    while (rs.next()) {
//                        String divisionType = rs.getString(1);
//                        int sectionNo = Integer.parseInt(divisionType);
//                        // by Huijuan, 09/20/2017, now we only use the sectionNo, can be replaced by x, y, z instead
//                        Coordinate coordinate = new Coordinate(sectionNo,sectionNo, sectionNo);
//                        Integer count = Integer.valueOf(rs.getInt(2));
//                        geoDistMap.put(coordinate, count);
//                    }
//
//                    System.out.println("rs is "+ rs);
//
//                    long endTime = System.currentTimeMillis();
//                    Long elapsed = Long.valueOf(endTime - startTime);
//                    System.out.println("the size of map is "+ geoDistMap.size());
//                    for (Coordinate key: geoDistMap.keySet()){
//                        System.out.println ("key: "+ key.x + " value: " + geoDistMap.get(key));
//                    }
//                    System.out.println("querying time of "+ selectSql + " "+ elapsed+" milliseconds. \n");
//
//                } finally {
//                    if (rs != null) {
//                        rs.close();
//                    }
//                }
//            } finally {
//                if (ps != null) {
//                    ps.close();
//                    activeStatements.remove(ps);
//                }
//            }
//        } finally {
//            if (conn != null) {
//                DataSourceUtils.releaseConnection(conn, dataSource);
//            }
//        }
//        return geoDistMap;
//    }





    public Map<String, Integer> findStringDistribution2(final String selectSql)
            throws SQLException {
        Connection conn = null;
        Map<String, Integer> strDistMap = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            if (explainPlans) {
                explainPlan2(selectSql, conn);
            }
            PreparedStatement ps = null;
            try {
                ps = conn
                        .prepareStatement(selectSql,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
                activeStatements.add(ps);
                ResultSet rs = null;
                try {
                    long startTime = System.currentTimeMillis();

                    rs = ps.executeQuery();
                    strDistMap = new HashMap<String, Integer>();
                    while (rs.next()) {
                        String str = rs.getString(1);
                        Integer count = Integer.valueOf(rs.getInt(2));
                        strDistMap.put(str, count);
                    }

                    System.out.println(" rs is "+ rs);

                    long endTime = System.currentTimeMillis();
                    Long elapsed = Long.valueOf(endTime - startTime);
                    System.out.println("the size of map is "+ strDistMap.size());
                    for (String key: strDistMap.keySet()){
                        System.out.println ("key: "+ key + " value: " + strDistMap.get(key));
                    }
                    System.out.println("querying time of "+ selectSql + " "+ elapsed+" milliseconds. \n");

                } finally {
                    if (rs != null) {
                        rs.close();
                    }
                }
            } finally {
                if (ps != null) {
                    ps.close();
                    activeStatements.remove(ps);
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }
        return strDistMap;
    }





    void explainPlan2(final String selectSql, Connection conn)
            throws SQLException {
        Statement s = null;
        try {
            s = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY);
            activeStatements.add(s);
            s.execute("SET SHOWPLAN_XML ON");
            if (s.execute(selectSql)) {
                ResultSet r = null;
                try {
                    long startTime = System.currentTimeMillis();

                    r = s.getResultSet();
                    r.next();
                    System.out.println(r.getString(1));
                    s.execute("SET SHOWPLAN_XML OFF");

                    long endTime = System.currentTimeMillis();
                    Long elapsed = Long.valueOf(endTime - startTime);
                    System.out.println("querying time of "+ selectSql + " "+ elapsed+" milliseconds. \n");
                } finally {
                    if (r != null) {
                        r.close();
                    }
                }
            }
        } finally {
            if (s != null) {
                s.close();
                activeStatements.remove(s);
            }
        }
    }

    @Override
    public Map<String, Integer> getDocumentTypeDistribution(int topCutoff)
            throws SQLException, IOException {
        return new HashMap<String, Integer>();
    }




/*
	@Override
	public Map<String, Integer> getDeceasedDistribution() throws SQLException,
			IOException {
		return findStringDistribution2(
				"SELECT tp.dischg_disp_code_desc  , COUNT(dischg_disp_code_desc) "+
						"FROM (   SELECT dis.dischg_disp_code_desc  "+
						" FROM  hf_jul_2016.hf_d_dischg_disp dis" +
						//" INNER JOIN  voogo." +  TEMP_PATIENT_TABLE + " s "+
						//" ON s.discharge_disposition_id = dis.dischg_disp_id " +
						" ) tp "+
						" GROUP BY tp.dischg_disp_code_desc  "
		);
	}
*/

//    @Override
//    public Map<Coordinate, Integer> getGeographicDistribution()
//            throws SQLException, IOException {
//        //return new HashMap<Coordinate, Integer>();
//        // add by Huijuan 9/20/2017
//
//
//        //select h.census_division, count(h.census_division) from voogo.hf_d_hospital h
//        //inner join voogo.tmp_pat_b6ad4f2a_2d60_4d09_945a_a45d79432225 t
//        //ON t.hospital_id = h.hospital_id
//        //group by h.census_division
//        //order by h.census_division
//
//        return findGeoDistribution(
//
//                "select h.census_division, count(h.census_division) from hf_jul_2016.hf_d_hospital h" +
//                        " inner join "+ TEMP_PATIENT_TABLE +
//                        " t ON t.hospital_id = h.hospital_id group by h.census_division order by h.census_division"
//        );
//    }
    /*
        @Override
        public List<CodeNameCount> getDiagnoses(String patientId)  throws SQLException {
            //updated by Huijuan 09/07/2017

            return findCodeCountsByPatient(
                    patientId,
            "SELECT icd.diagnosis_id, icd.diagnosis_code, icd.diagnosis_description, COUNT(*)"
            + " FROM hf_d_diagnosis icd "+
                    //" INNER JOIN hf_Jul_2016.hf_f_diagnosis_" + fieldValueMap.get(Field.ENCOUNTER_YEAR).get(0).term + " dx on icd.diagnosis_id = dx.diagnosis_id " +
                    " INNER JOIN hf_Jul_2016.hf_f_diagnosis dx on icd.diagnosis_id = dx.diagnosis_id " +
                    " INNER JOIN "+ TEMP_PATIENT_TABLE + " s on dx.encounter_id = s.encounter_id"
                    + " GROUP BY icd.diagnosis_id, icd.diagnosis_code, icd.diagnosis_description"
                    + " ORDER BY 3 DESC");

        String searchDiagnosisSQL= patientId;
        //Map<String, Integer> map = findStringDistribution2(searchDiagnosisSQL);
        List<CodeNameCount> results = new ArrayList<>();

        Connection conn = null;
        Map<String, Integer> strDistMap = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);

            if (explainPlans) {
                explainPlan2(searchDiagnosisSQL, conn);
            }
            PreparedStatement ps = null;
            try {
                ps = conn
                        .prepareStatement(searchDiagnosisSQL,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
                activeStatements.add(ps);
                ResultSet rs = null;
                try {
                    long startTime = System.currentTimeMillis();

                    rs = ps.executeQuery();
                    strDistMap = new HashMap<String, Integer>();
                    while (rs.next()) {
                        //String icd = rs.getString(1);
                        //String name = rs.getString(2);
                        //strDistMap.put(icd, name);
                        results.add(new CodeNameCount(rs.getString(1),rs.getString(2), Integer.valueOf(rs.getInt(3))));
                    }

                    results.sort(new Comparator<CodeNameCount>() {
                        @Override
                        public int compare(CodeNameCount o1, CodeNameCount o2) {
                            return o1.name.compareToIgnoreCase(o2.name);
                        }
                    });

                    System.out.println("rs is "+ rs);

                    long endTime = System.currentTimeMillis();
                    Long elapsed = Long.valueOf(endTime - startTime);
                    System.out.println("the size of map is "+ strDistMap.size());
                    for (String key: strDistMap.keySet()){
                        System.out.println ("key: "+ key + " value: " + strDistMap.get(key));
                    }
                    System.out.println("querying time of "+ searchDiagnosisSQL + " "+ elapsed+" milliseconds. \n");

                } finally {
                    if (rs != null) {
                        rs.close();
                    }
                }
            } finally {
                if (ps != null) {
                    ps.close();
                    activeStatements.remove(ps);
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }

        return results;
    }
*/





    /*
   public List<CodeNameCount> getICD9Code(String patientId)
           throws SQLException {
       //updated by Huijuan 09/07/2017

       return findCodeCountsByPatient(
               patientId,
       "SELECT icd.diagnosis_id, icd.diagnosis_code, icd.diagnosis_description, COUNT(*)"
       + " FROM hf_d_diagnosis icd "+
               " INNER JOIN hf_Jul_2016.hf_f_diagnosis_" + fieldValueMap.get(Field.ENCOUNTER_YEAR).get(0).term + " dx on icd.diagnosis_id = dx.diagnosis_id " +
               " INNER JOIN voogo."+ TEMP_PATIENT_TABLE + " s on dx.encounter_id = s.encounter_id"
               + " GROUP BY icd.diagnosis_id, icd.diagnosis_code, icd.diagnosis_description"
               + " ORDER BY 3 DESC");


       String searchDiagnosisSQL= patientId;
       //Map<String, Integer> map = findStringDistribution2(searchDiagnosisSQL);
       List<CodeNameCount> results = new ArrayList<>();

       Connection conn = null;
       Map<String, Integer> strDistMap = null;
       try {
           conn = DataSourceUtils.getConnection(dataSource);

           if (explainPlans) {
               explainPlan2(searchDiagnosisSQL, conn);
           }
           PreparedStatement ps = null;
           try {
               ps = conn
                       .prepareStatement(searchDiagnosisSQL,
                               ResultSet.TYPE_FORWARD_ONLY,
                               ResultSet.CONCUR_READ_ONLY);
               activeStatements.add(ps);
               ResultSet rs = null;
               try {
                   long startTime = System.currentTimeMillis();

                   rs = ps.executeQuery();
                   strDistMap = new HashMap<String, Integer>();
                   while (rs.next()) {
                       //String icd = rs.getString(1);
                       //String name = rs.getString(2);
                       //strDistMap.put(icd, name);
                       results.add(new CodeNameCount(rs.getString(1),rs.getString(2), Integer.valueOf(rs.getInt(3))));
                   }

                   System.out.println("rs is "+ rs);

                   long endTime = System.currentTimeMillis();
                   Long elapsed = Long.valueOf(endTime - startTime);
                   System.out.println("the size of map is "+ strDistMap.size());
                   for (String key: strDistMap.keySet()){
                       System.out.println ("key: "+ key + " value: " + strDistMap.get(key));
                   }
                   System.out.println("querying time of "+ searchDiagnosisSQL + " "+ elapsed+" milliseconds. \n");

               } finally {
                   if (rs != null) {
                       rs.close();
                   }
               }
           } finally {
               if (ps != null) {
                   ps.close();
                   activeStatements.remove(ps);
               }
           }
       } finally {
           if (conn != null) {
               DataSourceUtils.releaseConnection(conn, dataSource);
           }
       }

       return results;
   }

   @Override
   public Map<String, Integer> getRxDistribution(int topCutoff)
           throws SQLException, IOException, InterruptedException {
       Map<String, Integer> result=null;
       String db = "hf_jul_2016";

       //if (!fieldValueMap.containsKey(Field.DRUGS)){
       result= findStringDistribution2(
               //"select d.generic_name, count(d.generic_name) from hf_d_medication d inner join hf_f_medication f on f.medication_id=d.medication_id group by d.generic_name"
               "select generic_name, count(generic_name) from "+db+".hf_d_medication md inner join " +db +".hf_f_medication mf on md.medication_id = mf.medication_id " +" inner join "+ TEMP_PATIENT_TABLE +" t on mf.encounter_id= t.encounter_id group by md.generic_name"
       );

       if (result==null){
           result = new HashMap<String, Integer>();
           //Thread.sleep(1000);
       }

       return result;
   }

   @Override
   public Map<String, Integer> getDxDistribution(int topCutoff)
           throws SQLException, IOException, InterruptedException {
       String db = "hf_jul_2016";

       Map<String, Integer> result=findStringDistribution2(
               "SELECT condition_category, COUNT(condition_category)" +
                       " FROM "+db+".hf_d_diagnosis icd "+
                       " INNER JOIN " + db +".hf_f_diagnosis dx on icd.diagnosis_id = dx.diagnosis_id "+
                       "INNER JOIN " + TEMP_PATIENT_TABLE + " t ON dx.encounter_id = t.encounter_id " +
                       " GROUP BY icd.condition_category " +
                       " ORDER BY icd.condition_category ASC"
                       + " LIMIT " + topCutoff);
       if (result==null){
           result = new HashMap<String, Integer>();
       }
       return result;
   }

   */
    @Override
    public Map<String, Integer> getProcedureDistribution(int topCutoff)
            throws SQLException, IOException {
        //return new HashMap<String, Integer>();
        String db = "hf_jul_2016";

        return findStringDistribution2(
                "select d.lab_procedure_name, count(d.lab_procedure_name) " +
                        "from " + db + ".hf_d_lab_procedure d " +
                        "inner join " + db + ".hf_f_lab_procedure f ON f.detail_lab_procedure_id = d.lab_procedure_id " +
                        "inner join " + TEMP_PATIENT_TABLE+
                        " t ON t.encounter_id = f.encounter_id " +
                        "group by d.lab_procedure_name"
        );
    }


    //SEYED
    //PDiag

    public String getAllEncounters ()   // all encounters of a patient
            throws SQLException {

        String[] Esql=new String[4];



        Esql[0] = " CREATE temporary TABLE IF NOT EXISTS " + TEMP_ENCOUNTER_TABLE +  " AS  select e.patient_id, e.encounter_id,  e.admitted_dt_tm, e.discharged_dt_tm  from hf_jul_2016.hf_f_encounter e " +
                " inner join "  + TEMP_PATIENT_TABLE+ " tmp on e.patient_id= tmp.patient_id ";

        Esql[1]=  "CREATE INDEX IF NOT EXISTS ix_"+TEMP_ENCOUNTER_TABLE+"_encid ON " + TEMP_PATIENT_TABLE + " (encounter_id)";

        Esql[2]=  "CREATE INDEX IF NOT EXISTS ix_"+TEMP_ENCOUNTER_TABLE+"_patid ON " + TEMP_ENCOUNTER_TABLE + " (patient_id)";


        System.out.println(" SE S");

        executeStatements(Esql);
        System.out.println(" SE DONE");

        return Esql[0];
    }



    @Override
    public  List<Encounter> getEncounters(String patientId) throws SQLException, IOException {



        int mpatient= Integer.parseInt(patientId);

        String dSQL = "select distinct encounter_id, date(admitted_dt_tm) , " +
                "case when DATE_PART('day',discharged_dt_tm - admitted_dt_tm) = 0 then concat(DATE_PART('hour', discharged_dt_tm - admitted_dt_tm ), ' hours') " +
                "when DATE_PART('day',discharged_dt_tm - admitted_dt_tm) > 0 then" +
                " concat(DATE_PART('day',discharged_dt_tm - admitted_dt_tm) , ' days & ', DATE_PART('hour', discharged_dt_tm - admitted_dt_tm ), ' hours') end "+
                " from "+  TEMP_ENCOUNTER_TABLE  + "  where  patient_id in ("+mpatient+") order by date(admitted_dt_tm) ASC  ";


        final List<Encounter> encounters = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            PreparedStatement ps = null;
            try {
                ps = conn.prepareStatement(dSQL,
                        ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_READ_ONLY);
                activeStatements.add(ps);
                ResultSet rs = null;
                try {
                    rs = ps.executeQuery();
                } finally {
                    if (rs != null) {
                        while (rs.next()) {

                            final String text = rs.getString(2);
                            final String type = rs.getString(3);
                            final Encounter e = new Encounter(rs.getLong(1), text, type, null, null);
                            encounters.add(e);
                        }
                        rs.close();
                    }
                }
            } finally {
                if (ps != null) {
                    ps.close();
                    activeStatements.remove(ps);
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }

        return encounters;


    }




    public  List<String> select_Diagnosis(String q_diag) throws SQLException {


        String dSQL = " select distinct lower(str) from ( " +
                "                           select str " +
                "                           from hf_jul_2016.mrconso_diagnosis mrcon " +  // sab in (ICD9 , ICD10 , MESH)
                "                                  inner join ( " +
                "                             select cui2 " +
                "                             from hf_jul_2016.mrrel_diagnosis mrl " +
                "                                    inner join hf_jul_2016.mrconso_diagnosis  mrm " +
                "                                               on mrl.cui1 = mrm.cui and mrm.str::citext like '%"+ q_diag +"%' and rel = 'SY' " +
                "                           ) se on mrcon.cui = se.cui2 ) y " +
                "where str::citext  not like '%(finding)%' " +
                "  and str::citext  not like '%(disorder)%' " +
                "  and str::citext not like '%"+ q_diag +"%' " +
                "  and str::citext not like '%(%' " +
                "  and str::citext not like '%[%' " +
                "  and str::citext not like '%-%' " +
                "  and str::citext not like '%,%' " +
                "  and str::citext not like '%:%' " +
                "  and length(str) <30 " +
                "\n" +
                "order by lower(str) ";


        final List<String> rec_diag = new ArrayList<>();
        Connection conn = null;

        try {
            conn = DataSourceUtils.getConnection(dataSource);
            PreparedStatement ps = null;
            try {
                ps = conn
                        .prepareStatement(dSQL,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
                activeStatements.add(ps);
                ResultSet rs = null;
                try {
                    rs = ps.executeQuery();
                } finally {
                    if (rs != null) {
                        while (rs.next()) {

                            final String pd = (rs.getString(1));


                            rec_diag.add(pd);
                        }
                        rs.close();
                    }
                }
            } finally {
                if (ps != null) {
                    ps.close();
                    activeStatements.remove(ps);
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }

        return rec_diag;
    }



    public  List<String> select_Medication(String q_med) throws SQLException {


        String dSQL = " select distinct lower(str) from ( " +
                "                                  select str " +
                "                                  from hf_jul_2016.mrconso_medication mrcon " +  //create table hf_jul_2016.mrconso_medication as select * from umls.mrconso where sab = 'MSH' UNION select * from umls.mrconso  where sab = 'RXNORM'  and tty = 'IN' UNION select * from umls.mrconso where sab = 'NDFRT'
                "                                         inner join  ( " +
                "                                    select cui2 " +
                "                                    from hf_jul_2016.mrrel_medication mrl " +  //create table hf_jul_2016.mrrel_medication as select * from umls.mrrel where sab = 'MSH' and rel = 'CHD' UNION select * from umls.mrrel  where sab = 'RXNORM'  UNION select * from umls.mrrel where sab = 'NDFRT' and rel= 'RO' and rela='has_therapeutic_class'
                "                                           inner join hf_jul_2016.mrconso_medication mrm " +
                "                                                      on mrl.cui1 = mrm.cui and mrm.str::citext like '%"+ q_med+"%' " +
                "                                  ) se on mrcon.cui = se.cui2 ) y " +
                "                                                 where  str::citext not like '%"+ q_med+"  %' " +
                "                                                  and str::citext not like '%(%' " +
                "                                                  and str::citext not like '%[%' " +
                "                                                   and str::citext not like '%/%' " +
                "                                                   and str::citext not like '%-%' " +
                "                                                   and str::citext not like '%mg%' " +
                "                                                   and length(str) <30 " +
                "order by lower(str) ";


        final List<String> rec_diag = new ArrayList<>();
        Connection conn = null;

        try {
            conn = DataSourceUtils.getConnection(dataSource);
            PreparedStatement ps = null;
            try {
                ps = conn
                        .prepareStatement(dSQL,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
                activeStatements.add(ps);
                ResultSet rs = null;
                try {
                    rs = ps.executeQuery();
                } finally {
                    if (rs != null) {
                        while (rs.next()) {

                            final String pd = (rs.getString(1));


                            rec_diag.add(pd);
                        }
                        rs.close();
                    }
                }
            } finally {
                if (ps != null) {
                    ps.close();
                    activeStatements.remove(ps);
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }

        return rec_diag;
    }






    @Override
    public  List<PDiag> getDiagnoses(String patientId) throws SQLException {

        int mpatient= Integer.parseInt(patientId);



        String dSQL = "Select dd.diagnosis_description ,tt.admitted_dt_tm, tt.discharged_dt_tm   " +
                "from "+ TEMP_PATIENT_TABLE +" tt inner join hf_jul_2016.hf_f_diagnosis fd on tt.encounter_id=fd.encounter_id and tt.patient_id = " +mpatient+
                "inner join hf_jul_2016.hf_d_diagnosis dd on dd.diagnosis_id=fd.diagnosis_id";


//        String dSQL = "Select dd.diagnosis_description ,tt.admitted_dt_tm, tt.discharged_dt_tm   " +
//                "from "+ TEMP_PATIENT_TABLE +" tt inner join hf_jul_2016.hf_f_diagnosis fd on tt.encounter_id=fd.encounter_id and tt.patient_id = " +mpatient+
//                "inner join hf_jul_2016.hf_d_diagnosis dd on dd.diagnosis_id=fd.diagnosis_id";

//        String dSQL = " Select diagnosis_description, admitted_dt_tm, discharged_dt_tm   " +
//                " from  " + TEMP_DIAGNOSIS_TABLE + " dtmp inner join " + TEMP_PATIENT_TABLE + " ptmp " +
//                " on dtmp.encounter_id= ptmp.encounter_id and patient_id = " +mpatient ;



        final List<PDiag> pdiag = new ArrayList<>();
        Connection conn = null;

        try {
            conn = DataSourceUtils.getConnection(dataSource);
            PreparedStatement ps = null;
            try {
                ps = conn
                        .prepareStatement(dSQL,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
                activeStatements.add(ps);
                ResultSet rs = null;
                try {
                    rs = ps.executeQuery();
                } finally {
                    if (rs != null) {
                        while (rs.next()) {

                            final PDiag pd = new PDiag(rs.getString(1), rs.getTimestamp(2), rs.getTimestamp(3));



                            pdiag.add(pd);
                        }
                        rs.close();
                    }
                }
            } finally {
                if (ps != null) {
                    ps.close();
                    activeStatements.remove(ps);
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }

        return pdiag;


        //   return new ArrayList<Encounter>(0);
    }









    public  List<PDiag> getDiagnoses2(String patientId) throws SQLException {   // for all comorbid conditions in All Encounter

        int mpatient= Integer.parseInt(patientId);





        String dSQL = "Select dd.diagnosis_description, e.admitted_dt_tm, e.discharged_dt_tm   " +
                "from " + TEMP_ENCOUNTER_TABLE + " e inner join hf_jul_2016.hf_f_diagnosis fd on e.encounter_id=fd.encounter_id and e.patient_id = " +mpatient+
                "inner join hf_jul_2016.hf_d_diagnosis dd on dd.diagnosis_id=fd.diagnosis_id";

//        String dSQL = "Select dd.diagnosis_description, e.admitted_dt_tm, e.discharged_dt_tm   " +
//                "from hf_jul_2016.hf_f_encounter e inner join hf_jul_2016.hf_f_diagnosis fd on e.encounter_id=fd.encounter_id and e.patient_id = " +mpatient+
//                "inner join hf_jul_2016.hf_d_diagnosis dd on dd.diagnosis_id=fd.diagnosis_id";






        final List<PDiag> pdiag = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            PreparedStatement ps = null;
            try {
                ps = conn
                        .prepareStatement(dSQL,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
                activeStatements.add(ps);
                ResultSet rs = null;
                try {
                    rs = ps.executeQuery();
                } finally {
                    if (rs != null) {
                        while (rs.next()) {
                            //  final String Di = rs.getString(1);
                            final PDiag pd = new PDiag(rs.getString(1), rs.getTimestamp(2), rs.getTimestamp(3));



                            pdiag.add(pd);
                        }
                        rs.close();
                    }
                }
            } finally {
                if (ps != null) {
                    ps.close();
                    activeStatements.remove(ps);
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }

        return pdiag;


        //   return new ArrayList<Encounter>(0);
    }








//    @Override
//    public List<Encounter> getEncounters(String patientId) throws SQLException,
//            CorruptIndexException, IOException {
//        return new ArrayList<Encounter>(0);
//    }



    public  List<PDiag> matching_encounter_condition (String enc_id) throws SQLException {
        int enc_id_int= Integer.parseInt(enc_id);
        String dSQL = "Select dd.diagnosis_description ,e.admitted_dt_tm, e.discharged_dt_tm from  " +  TEMP_PATIENT_TABLE   + " e inner join " +
                " hf_jul_2016.hf_f_diagnosis fd on e.encounter_id=fd.encounter_id and e.encounter_id = " +enc_id_int+
                "inner join hf_jul_2016.hf_d_diagnosis dd on dd.diagnosis_id=fd.diagnosis_id" ;


        final List<PDiag> pdiag = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            PreparedStatement ps = null;
            try {
                ps = conn
                        .prepareStatement(dSQL,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
                activeStatements.add(ps);
                ResultSet rs = null;
                try {
                    rs = ps.executeQuery();
                } finally {
                    if (rs != null) {
                        while (rs.next()) {

                            final PDiag pd = new PDiag(rs.getString(1), rs.getTimestamp(2), rs.getTimestamp(3));



                            pdiag.add(pd);
                        }
                        rs.close();
                    }
                }
            } finally {
                if (ps != null) {
                    ps.close();
                    activeStatements.remove(ps);
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }

        return pdiag;


        //   return new ArrayList<Encounter>(0);
    }



    public  List<PDiag> all_encounter_condition (String enc_id) throws SQLException {
        int enc_id_int= Integer.parseInt(enc_id);
        String dSQL = "Select dd.diagnosis_description ,e.admitted_dt_tm, e.discharged_dt_tm from  " +  TEMP_ENCOUNTER_TABLE   + " e inner join " +
                " hf_jul_2016.hf_f_diagnosis fd on e.encounter_id=fd.encounter_id and e.encounter_id = " +enc_id_int+
                "inner join hf_jul_2016.hf_d_diagnosis dd on dd.diagnosis_id=fd.diagnosis_id" ;


        final List<PDiag> pdiag = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            PreparedStatement ps = null;
            try {
                ps = conn
                        .prepareStatement(dSQL,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
                activeStatements.add(ps);
                ResultSet rs = null;
                try {
                    rs = ps.executeQuery();
                } finally {
                    if (rs != null) {
                        while (rs.next()) {

                            final PDiag pd = new PDiag(rs.getString(1), rs.getTimestamp(2), rs.getTimestamp(3));



                            pdiag.add(pd);
                        }
                        rs.close();
                    }
                }
            } finally {
                if (ps != null) {
                    ps.close();
                    activeStatements.remove(ps);
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }

        return pdiag;


        //   return new ArrayList<Encounter>(0);
    }
















    @Override
    public  List<Encounter> getMatchingEncounters(String patientId, List<SearchTerm> searchTerms) throws SQLException, IOException {  // query to return encounters in the matchig encounters

        int mpatient= Integer.parseInt(patientId);

        String dSQL = "select distinct encounter_id, date(admitted_dt_tm) , case when DATE_PART('day',discharged_dt_tm - admitted_dt_tm) = 0 then concat(DATE_PART('hour', discharged_dt_tm - admitted_dt_tm ), ' hours') " +
                "when DATE_PART('day',discharged_dt_tm - admitted_dt_tm) > 0 then concat(DATE_PART('day',discharged_dt_tm - admitted_dt_tm) , ' days & ', DATE_PART('hour', discharged_dt_tm - admitted_dt_tm ), ' hours') end "+
                "from "+ TEMP_PATIENT_TABLE +"  where  patient_id in ("+mpatient+")  order by date(admitted_dt_tm) asc   ";



        final List<Encounter> encounters = new ArrayList<>();
        Connection conn = null;

        try {
            conn = DataSourceUtils.getConnection(dataSource);
            PreparedStatement ps = null;
            try {
                ps = conn
                        .prepareStatement(dSQL,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
                activeStatements.add(ps);
                ResultSet rs = null;
                try {
                    rs = ps.executeQuery();
                } finally {
                    if (rs != null) {
                        while (rs.next()) {
                            //   final long id = rs.getLong(1);

                            final String text = rs.getString(2);
                            final String type = rs.getString(3);
                            final Encounter e = new Encounter(rs.getLong(1), text, type, null, null);
                            encounters.add(e);
                        }
                        rs.close();
                    }
                }
            } finally {
                if (ps != null) {
                    ps.close();
                    activeStatements.remove(ps);
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }

        return encounters;


        //   return new ArrayList<Encounter>(0);
    }














//Seyed

    @Override
    public List<Patient> getPatients(int page, int pagesize) throws SQLException, IOException {

        /// patient id from temp
        String dSQL = "Select distinct dp.patient_id, dp.gender, dp.race, date_part('year', admitted_dt_tm)- tmt.age_in_years from "+
                TEMP_PATIENT_TABLE+" tmt inner join hf_jul_2016.hf_d_patient dp on tmt.patient_id=dp.patient_id and dp.race not in ('NULL','Null','Unknown', 'Other' )"+
                " LIMIT " +  pagesize + " offset " + pagesize* (abs((page-1)));
        final List<Patient> patients = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            PreparedStatement ps = null;
            try {
                ps = conn
                        .prepareStatement(dSQL,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
                activeStatements.add(ps);
                ResultSet rs = null;
                try {
                    rs = ps.executeQuery();
                } finally {
                    if (rs != null) {
                        while (rs.next()) {
                            final String patientId= rs.getString(1);
                            final String gender = rs.getString(2);
                            final String race = rs.getString(3);
                            final String year = rs.getString(4);
                            final Patient p = new Patient(patientId, null, gender, year, race, null, null, null, null, null);
                            patients.add(p);
                        }
                        rs.close();
                    }
                }
            } finally {
                if (ps != null) {
                    ps.close();
                    activeStatements.remove(ps);
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }

        return patients;


    }

//    @Override
//    public List<CodeNameCount> getProcedures(String patientId) throws SQLException {
//        //return new ArrayList<CodeNameCount>(0);
//        String searchDiagnosisSQL= patientId;
//        //Map<String, Integer> map = findStringDistribution2(searchDiagnosisSQL);
//        List<CodeNameCount> results = new ArrayList<>();
//
//        Connection conn = null;
//        Map<String, Integer> strDistMap = null;
//        try {
//            conn = DataSourceUtils.getConnection(dataSource);
//
//            if (explainPlans) {
//                explainPlan2(searchDiagnosisSQL, conn);
//            }
//            PreparedStatement ps = null;
//            try {
//                ps = conn
//                        .prepareStatement(searchDiagnosisSQL,
//                                ResultSet.TYPE_FORWARD_ONLY,
//                                ResultSet.CONCUR_READ_ONLY);
//                activeStatements.add(ps);
//                ResultSet rs = null;
//                try {
//                    long startTime = System.currentTimeMillis();
//
//                    rs = ps.executeQuery();
//                    strDistMap = new HashMap<String, Integer>();
//                    while (rs.next()) {
//                        //String icd = rs.getString(1);
//                        //String name = rs.getString(2);
//                        //strDistMap.put(icd, name);
//                        results.add(new CodeNameCount(rs.getString(1),rs.getString(2), Integer.valueOf(rs.getInt(3))));
//                    }
//
//                    results.sort(new Comparator<CodeNameCount>() {
//                        @Override
//                        public int compare(CodeNameCount o1, CodeNameCount o2) {
//                            return o1.name.compareToIgnoreCase(o2.name);
//                        }
//                    });
//
//                    System.out.println("rs is "+ rs);
//
//                    long endTime = System.currentTimeMillis();
//                    Long elapsed = Long.valueOf(endTime - startTime);
//                    System.out.println("the size of map is "+ strDistMap.size());
//                    for (String key: strDistMap.keySet()){
//                        System.out.println ("key: "+ key + " value: " + strDistMap.get(key));
//                    }
//                    System.out.println("querying time of "+ searchDiagnosisSQL + " "+ elapsed+" milliseconds. \n");
//
//                } finally {
//                    if (rs != null) {
//                        rs.close();
//                    }
//                }
//            } finally {
//                if (ps != null) {
//                    ps.close();
//                    activeStatements.remove(ps);
//                }
//            }
//        } finally {
//            if (conn != null) {
//                DataSourceUtils.releaseConnection(conn, dataSource);
//            }
//        }
//
//        return results;
//    }


////////////////////////////////////////////////////////               Lab Results       ///////////////////////////////////////////////////////////////////////////


    @Override
    public List<PLResult> getLabResult(String patientId) throws SQLException {    // get all lab results for the matching encounters





        String dSQL = "select l.lab_procedure_mnemonic, l.lab_procedure_group , l.lab_drawn_dt_tm , l.numeric_result ,l.mysort , l.Status " +
                " from hf_jul_2016.hf_f_lab_procedure_result_full l   " +
                " inner join " + TEMP_PATIENT_TABLE+ " tpt "
                + "on tpt.encounter_id=l.encounter_id and  tpt.patient_id = " +patientId+ " order by   lab_procedure_mnemonic asc, lab_drawn_dt_tm asc ";








        final List<PLResult> plr = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            PreparedStatement ps = null;
            try {
                ps = conn
                        .prepareStatement(dSQL,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
                activeStatements.add(ps);
                ResultSet rs = null;
                try {
                    rs = ps.executeQuery();
                } finally {
                    if (rs != null) {
                        while (rs.next()) {

                            final PLResult pl = new PLResult(rs.getString(1),rs.getString(2), rs.getDate(3), rs.getFloat(4), rs.getByte(5), rs.getByte(6));
                            plr.add(pl);
                        }
                        rs.close();
                    }
                }
            } finally {
                if (ps != null) {
                    ps.close();
                    activeStatements.remove(ps);
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }

        return plr;



    }



    @Override
    public List<PLResult> getLabResult_ALL_Matches(String patientId) throws SQLException {  //get lab results for  all encounters



        String dSQL = " select l.lab_procedure_mnemonic, l.lab_procedure_group, l.lab_drawn_dt_tm , l.numeric_result ,l.mysort, l.Status " +
                " from hf_jul_2016.hf_f_lab_procedure_result_full l  " +
                " inner join  " +  TEMP_ENCOUNTER_TABLE + "  tpt "
                + " on tpt.encounter_id=l.encounter_id and  tpt.patient_id = " +patientId+ " order by  lab_procedure_mnemonic asc, lab_drawn_dt_tm asc ";



        final List<PLResult> plr = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            PreparedStatement ps = null;
            try {
                ps = conn
                        .prepareStatement(dSQL,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
                activeStatements.add(ps);
                ResultSet rs = null;
                try {

                    rs = ps.executeQuery();

                } finally {

                    if (rs != null) {
                        while (rs.next()) {
                            //  final String PL = rs.getString(1);
                            final PLResult pl = new PLResult(rs.getString(1),rs.getString(2), rs.getDate(3), rs.getFloat(4), rs.getByte(5), rs.getByte(6));
                            plr.add(pl);
                        }
                        rs.close();
                    }
                }
            } finally {
                if (ps != null) {
                    ps.close();
                    activeStatements.remove(ps);
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }
        return plr;

    }



    @Override
    public List<PLResult> encounter_lab(String enc_id) throws SQLException {   //get lab results for each encounter in the matching encounters
        int enc_id_int= Integer.parseInt(enc_id);


        String dSQL = "select l.lab_procedure_mnemonic, l.lab_procedure_group, l.lab_drawn_dt_tm , l.numeric_result ,l.mysort , l.Status " +
                " from hf_jul_2016.hf_f_lab_procedure_result_full l  " +
                " inner join  " + TEMP_PATIENT_TABLE + " tpt "
                + "  on tpt.encounter_id=l.encounter_id and  l.encounter_id = " + enc_id_int+ " order by   lab_procedure_mnemonic asc, lab_drawn_dt_tm asc ";




        final List<PLResult> plr = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            PreparedStatement ps = null;
            try {
                ps = conn
                        .prepareStatement(dSQL,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
                activeStatements.add(ps);
                ResultSet rs = null;
                try {
                    rs = ps.executeQuery();
                } finally {
                    if (rs != null) {
                        while (rs.next()) {

                            final PLResult pl = new PLResult(rs.getString(1), rs.getString(2),rs.getDate(3), rs.getFloat(4), rs.getByte(5), rs.getByte(6));
                            plr.add(pl);
                        }
                        rs.close();
                    }
                }
            } finally {
                if (ps != null) {
                    ps.close();
                    activeStatements.remove(ps);
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }
        return plr;

    }


    @Override
    public List<PLResult> all_encounter_lab(String enc_id) throws SQLException {   //get lab results for each encounter in the all encounters
        int enc_id_int= Integer.parseInt(enc_id);

        String dSQL = " select l.lab_procedure_mnemonic, l.lab_procedure_group, l.lab_drawn_dt_tm , l.numeric_result , l.mysort , l.Status " +
                " from hf_jul_2016.hf_f_lab_procedure_result_full l  " +
                " inner join  "+  TEMP_ENCOUNTER_TABLE + "  tpt "
                + " on tpt.encounter_id= l.encounter_id  and tpt.encounter_id = " +enc_id_int+ "  order by  lab_procedure_mnemonic asc, lab_drawn_dt_tm asc ";



//        String dSQL = "select dl.lab_procedure_mnemonic, fl.lab_drawn_dt_tm , fl.numeric_result ,dl.sort , fl.Status " +
//                "from hf_jul_2016.hf_f_lab_procedure_result_2 fl inner join" +
//                "  hf_jul_2016.hf_d_lab_procedure_test dl on fl.detail_lab_procedure_id = dl.lab_procedure_id " +
//                " inner join  "+  TEMP_ENCOUNTER_TABLE + "  tpt "
//                + " on tpt.encounter_id=fl.encounter_id  and tpt.encounter_id = " +enc_id_int+ "  order by sort DESC ";

        final List<PLResult> plr = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            PreparedStatement ps = null;
            try {
                ps = conn
                        .prepareStatement(dSQL,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
                activeStatements.add(ps);
                ResultSet rs = null;
                try {
                    rs = ps.executeQuery();
                } finally {
                    if (rs != null) {
                        while (rs.next()) {
                            //  final String PL = rs.getString(1);
                            final PLResult pl = new PLResult(rs.getString(1), rs.getString(2),rs.getDate(2), rs.getFloat(3), rs.getByte(4), rs.getByte(5));
                            plr.add(pl);
                        }
                        rs.close();
                    }
                }
            } finally {
                if (ps != null) {
                    ps.close();
                    activeStatements.remove(ps);
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }
        return plr;

    }










//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////






//    @Override
//    public void cancel() throws SQLException {
//        // TODO Auto-generated method stub
//
//    }
//
//    @Override
//    public List<Encounter> getDocumentSample(int numPatients, boolean ranked)
//            throws SQLException, IOException {
//        return new ArrayList<Encounter>(0);
//    }
//
//    @Override
//    public void setDataSource(DataSource dataSource) {
//        this.dataSource = dataSource;
//    }
//
//}



    //   @Override
    public List<PMedication> matching_encounter_drugs(String enc_id) throws SQLException {
        int enc_id_int= Integer.parseInt(enc_id);
        String dSQL = "Select dm.generic_name, fm.med_started_dt_tm, fm.med_stopped_dt_tm , case when med_started_dt_tm <= med_stopped_dt_tm then 1 else 0 end AS Timing" +
                " from hf_jul_2016.hf_d_medication dm "
                +"inner join hf_jul_2016.hf_f_medication fm on dm.medication_id=fm.medication_id inner join " + TEMP_PATIENT_TABLE+ " tpt "
                + "on tpt.encounter_id=fm.encounter_id and fm.encounter_id = " +enc_id_int;

        final List<PMedication> pmed = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            PreparedStatement ps = null;
            try {
                ps = conn
                        .prepareStatement(dSQL,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
                activeStatements.add(ps);
                ResultSet rs = null;
                try {
                    rs = ps.executeQuery();
                } finally {
                    if (rs != null) {
                        while (rs.next()) {
                            final PMedication pm = new PMedication(rs.getString(1), rs.getDate(2), rs.getDate(3), rs.getByte(4) );
                            pmed.add(pm);
                        }
                        rs.close();
                    }
                }
            } finally {
                if (ps != null) {
                    ps.close();
                    activeStatements.remove(ps);
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }

        return pmed;

    }





    public List<PMedication> all_encounter_drugs(String enc_id) throws SQLException {  // Medications for each encounter in All Encounter
        int enc_id_int= Integer.parseInt(enc_id);
        String dSQL = "Select dm.generic_name, fm.med_started_dt_tm, fm.med_stopped_dt_tm , case when med_started_dt_tm <= med_stopped_dt_tm then 1 else 0 end AS Timing" +
                " from hf_jul_2016.hf_d_medication dm "
                +"inner join hf_jul_2016.hf_f_medication fm on dm.medication_id=fm.medication_id inner join " + TEMP_ENCOUNTER_TABLE + " tpt "
                + "on tpt.encounter_id=fm.encounter_id and fm.encounter_id = " +enc_id_int;

        final List<PMedication> pmed = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            PreparedStatement ps = null;
            try {
                ps = conn
                        .prepareStatement(dSQL,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
                activeStatements.add(ps);
                ResultSet rs = null;
                try {
                    rs = ps.executeQuery();
                } finally {
                    if (rs != null) {
                        while (rs.next()) {
                            final PMedication pm = new PMedication(rs.getString(1), rs.getDate(2), rs.getDate(3), rs.getByte(4) );
                            pmed.add(pm);
                        }
                        rs.close();
                    }
                }
            } finally {
                if (ps != null) {
                    ps.close();
                    activeStatements.remove(ps);
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }

        return pmed;

    }



    @Override
    public List<PMedication> getDrugs(String patientId) throws SQLException {  // All medications of a patient in Matching Encounter

        String dSQL = "Select dm.generic_name, fm.med_started_dt_tm, fm.med_stopped_dt_tm , case when med_started_dt_tm <= med_stopped_dt_tm then 1 else 0 end AS Timing" +
                " from hf_jul_2016.hf_d_medication dm "
                +"inner join hf_jul_2016.hf_f_medication fm on dm.medication_id=fm.medication_id inner join " + TEMP_PATIENT_TABLE+ " tpt "
                + "on tpt.encounter_id=fm.encounter_id where  tpt.patient_id = " +patientId;

        final List<PMedication> pmed = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            PreparedStatement ps = null;
            try {
                ps = conn
                        .prepareStatement(dSQL,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
                activeStatements.add(ps);
                ResultSet rs = null;
                try {
                    rs = ps.executeQuery();
                } finally {
                    if (rs != null) {
                        while (rs.next()) {
                            final PMedication pm = new PMedication(rs.getString(1), rs.getDate(2), rs.getDate(3), rs.getByte(4) );
                            pmed.add(pm);
                        }
                        rs.close();
                    }
                }
            } finally {
                if (ps != null) {
                    ps.close();
                    activeStatements.remove(ps);
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }

        return pmed;

    }



    //  @Override
    public List<PMedication> getDrugs2(String patientId) throws SQLException { //All medications of a patient in All Encounter

        String dSQL = "Select dm.generic_name, fm.med_started_dt_tm, fm.med_stopped_dt_tm ,  case when med_started_dt_tm <= med_stopped_dt_tm then 1 else 0 end AS Timing " +
                "from hf_jul_2016.hf_d_medication dm "
                +"inner join hf_jul_2016.hf_f_medication fm on dm.medication_id=fm.medication_id inner join " + TEMP_ENCOUNTER_TABLE
                + " e on e.encounter_id=fm.encounter_id where  e.patient_id = " +patientId;

        final List<PMedication> pmed = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            PreparedStatement ps = null;
            try {
                ps = conn
                        .prepareStatement(dSQL,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
                activeStatements.add(ps);
                ResultSet rs = null;
                try {
                    rs = ps.executeQuery();
                } finally {
                    if (rs != null) {
                        while (rs.next()) {
                            final PMedication pm = new PMedication(rs.getString(1), rs.getDate(2), rs.getDate(3), rs.getByte(4));
                            pmed.add(pm);
                        }
                        rs.close();
                    }
                }
            } finally {
                if (ps != null) {
                    ps.close();
                    activeStatements.remove(ps);
                }
            }
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }

        return pmed;

    }




    @Override
    public void cancel() throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public List<Encounter> getDocumentSample(int numPatients, boolean ranked)
            throws SQLException, IOException {
        return new ArrayList<Encounter>(0);
    }

    @Override
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

}
