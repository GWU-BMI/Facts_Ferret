/*
 *  Copyright 2011 United States Department of Veterans Affairs,
 *		Health Services Research & Development Service
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package gov.va.research.ir.model;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

import org.springframework.util.StringUtils;

/**
 * @author doug
 *
 */
public class SearchResult {


    public static class Patient {
        final public String id;
        final public Integer age;
        final public String gender;
        final public String year;
        final public String race;
        final public String state;
        final public String deceased;
        final public List<String> icd9s;
        final public List<String> drugs;
        final public List<String> procedures;
        public Patient(final String id, final Integer age, final String gender, String year,final String race, final String state, final String deceased, final List<String> icd9s, final List<String> drugs, final List<String> procedures) {
            this.id = id;
            this.age = age;
            this.gender = gender;
            this.year= year;
            this.race = race;
            this.state = state;
            this.deceased = deceased;
            this.icd9s = icd9s;
            this.drugs = drugs;
            this.procedures = procedures;
        }
        public Patient(final String patientStr) {
            String[] fields = patientStr.split("\\|");
            this.id = fields[0];
            this.age = Integer.valueOf(fields[1]);
            this.gender = fields[2];
            this.year=fields[3];
            this.race = fields[4];
            this.state = fields[5];
            this.deceased = fields[6];
            this.icd9s = null; //Arrays.asList(StringUtils.commaDelimitedListToStringArray(fields[6]));
            this.drugs = null; //Arrays.asList(StringUtils.commaDelimitedListToStringArray(fields[7]));
            this.procedures = null; //Arrays.asList(StringUtils.commaDelimitedListToStringArray(fields[8]));
        }
        @Override
        public String toString() {
            // return id + "|" + age + "|" + gender + "|" + county + "|" + state + "|" + deceased + "|" + StringUtils.collectionToCommaDelimitedString(icd9s) + "|" + StringUtils.collectionToCommaDelimitedString(drugs) + "|" + StringUtils.collectionToCommaDelimitedString(procedures);

            return  race ;
        }

    }

    public static class Encounter {
        final public Long id;
        final public String text;
        final public String type;
        final public Date date;
        final public String patientId;
        public Encounter(final Long id, final String text, final String type, final Date date, final String patientId) {
            this.id = id;
            this.text = text;
            this.type = type;
            this.date = date;
            this.patientId = patientId;
        }


        @Override
        public String toString() {

             return "  "+text + "     Length of stay: " + type  ;
        }

    }




    public static class PDiag {
        final public String Di;
        final public Timestamp date_adm;
        final public Timestamp date_dis;


        public PDiag(final String Di, final Timestamp date_adm, final Timestamp date_dis) {

            this.Di = Di;
            this.date_adm=date_adm;
            this.date_dis=date_dis;

        }

        public String getDi() {
            return Di;
        }

        public Timestamp getDateAdm() {
            return date_adm;
        }

        public Timestamp getDateDis() {
            return date_dis;
        }

//        @Override
//        public String toString() {
//
//           return Di ;
//        }


    }




    public static class PMedication {
        final public String PatMed;
        final public Date med_started_dt_tm;
        final public Date med_stopped_dt_tm;
        final public Byte Timing;



        public PMedication(final String PatMed, final Date med_started_dt_tm, final Date med_stopped_dt_tm, Byte Timing) {

            this.PatMed = PatMed;
            this.med_started_dt_tm=med_started_dt_tm;
            this.med_stopped_dt_tm=med_stopped_dt_tm;
            this.Timing= Timing;
        }



        public String getPatMed() {
            return PatMed;
        }

        public Date getDateAdm() {
            return med_started_dt_tm;
        }

        public Date getDateDis() {
            return med_stopped_dt_tm;
        }


        public Byte Timing() {
            return Timing;
        }


//        @Override
//        public String toString() {
//            return PatMed;
//        }
    }






    public static class PLResult {
        final public String PLR;
        final public String Group;
        final public Date lab_drawn_dt_tm;
        final public float numeric_result;
        final public byte sort;
        final public int Status;


        public PLResult(final String PLR, final String Group, final Date lab_drawn_dt_tm, final float numeric_result, final byte sort, final int Status ) {
            this.PLR = PLR;
            this.Group = Group;
            this.lab_drawn_dt_tm=lab_drawn_dt_tm;
            this.numeric_result=numeric_result;
            this.sort= sort;
            this.Status=Status;
        }



        public String getPLR() {

        return PLR;
        }

        public String getGroup() {

            return Group;
        }
        public Date getLabDate() {

            return lab_drawn_dt_tm;
        }





        public float getValue() {

            return numeric_result;
        }

        public byte getSort() {

            return sort;
        }

        public int getStatus() {

            return Status;
        }

//        @Override
//        public String toString() {
//            return PLR;
//        }
    }




    public static class MyList {
         public long lab_drawn_dt_tm;
         public float numeric_result;


        public MyList(final long lab_drawn_dt_tm,final float numeric_result) {

            this.lab_drawn_dt_tm = lab_drawn_dt_tm;
            this.numeric_result = numeric_result;
        }


        public long getLabDate2() {

            return lab_drawn_dt_tm;
        }



        public MyList(final long lab_drawn_dt_tm) {

            this.lab_drawn_dt_tm= lab_drawn_dt_tm ;
        }


        public MyList(float numeric_result) {

            this.numeric_result=numeric_result;
        }

    }





    public static class PatientEncounterComparator implements Comparator<SearchResult.Encounter> {

        /* (non-Javadoc)
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare(Encounter o1, Encounter o2) {
            int resultP = compare(
                    (o1 == null ? null : Long.valueOf(o1.patientId)),
                    (o2 == null ? null : Long.valueOf(o2.patientId)));
            if (resultP != 0) {
                return resultP;
            }
            return compare(
                    (o1 == null ? null : o1.id),
                    (o2 == null ? null : o2.id));
        }

        private int compare(Long l1, Long l2) {
            int result = 0;
            if (l1 == null) {
                if (l2 == null) {
                    result = 0;
                } else {
                    result = -1;
                }
            } else {
                if (l2 == null) {
                    result = 1;
                } else {
                    result = l1.compareTo(l2);
                }
            }
            return result;
        }
    }

}
