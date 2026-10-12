package data;

import models.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SampleData {
    public static List<Hospital> getHospitals() {
        List<Hospital> hospitals = new ArrayList<>();

        // Hospital 1: Main City Hospital (High Capacity, Specializes in everything)
        Hospital h1 = new Hospital(1, "City Central Hospital", "Downtown", 17.3850, 78.4867);
        for(int i = 1; i <= 6; i++) h1.addBed(new Bed(i, 1, Bed.BedType.GENERAL));
        for(int i = 7; i <= 9; i++) h1.addBed(new Bed(i, 1, Bed.BedType.ICU));
        h1.addOperatingRoom(new OperatingRoom(1, 1, "OR-A (General)", Arrays.asList(Patient.SurgeryType.GENERAL, Patient.SurgeryType.ENT, Patient.SurgeryType.ORTHOPEDIC)));
        h1.addOperatingRoom(new OperatingRoom(2, 1, "OR-B (Cardiac)", Arrays.asList(Patient.SurgeryType.CARDIAC)));
        h1.addOperatingRoom(new OperatingRoom(3, 1, "OR-C (Neuro)", Arrays.asList(Patient.SurgeryType.NEUROLOGICAL)));
        h1.addStaff(new Staff(1, "Dr. Smith", Staff.Role.SURGEON, 1, Arrays.asList(Patient.SurgeryType.CARDIAC, Patient.SurgeryType.GENERAL, Patient.SurgeryType.ENT, Patient.SurgeryType.ORTHOPEDIC), 480, 1020)); // 8am-17pm
        h1.addStaff(new Staff(2, "Dr. Jones", Staff.Role.SURGEON, 1, Arrays.asList(Patient.SurgeryType.NEUROLOGICAL), 540, 1080)); // 9am-18pm
        hospitals.add(h1);

        // Hospital 2: Suburban Medical Center (Medium Capacity, Ortho/General focus)
        Hospital h2 = new Hospital(2, "Suburban Medical", "Westville", 17.4350, 78.3867);
        for(int i = 10; i <= 14; i++) h2.addBed(new Bed(i, 2, Bed.BedType.GENERAL));
        h2.addBed(new Bed(15, 2, Bed.BedType.ICU));
        h2.addOperatingRoom(new OperatingRoom(4, 2, "OR-D (Ortho)", Arrays.asList(Patient.SurgeryType.ORTHOPEDIC, Patient.SurgeryType.GENERAL)));
        h2.addOperatingRoom(new OperatingRoom(5, 2, "OR-E (Eye/ENT)", Arrays.asList(Patient.SurgeryType.OPHTHALMIC, Patient.SurgeryType.ENT)));
        h2.addStaff(new Staff(3, "Dr. Miller", Staff.Role.SURGEON, 2, Arrays.asList(Patient.SurgeryType.ORTHOPEDIC, Patient.SurgeryType.GENERAL, Patient.SurgeryType.OPHTHALMIC, Patient.SurgeryType.ENT), 480, 960)); // 8am-16pm
        hospitals.add(h2);

        // Hospital 3: Regional Trauma Center (Emergency focus)
        Hospital h3 = new Hospital(3, "Regional Trauma Center", "Eastside", 17.3550, 78.5267);
        for(int i = 16; i <= 18; i++) h3.addBed(new Bed(i, 3, Bed.BedType.GENERAL));
        for(int i = 19; i <= 22; i++) h3.addBed(new Bed(i, 3, Bed.BedType.ICU));
        h3.addOperatingRoom(new OperatingRoom(6, 3, "Trauma OR-1", Arrays.asList(Patient.SurgeryType.GENERAL, Patient.SurgeryType.ORTHOPEDIC, Patient.SurgeryType.NEUROLOGICAL)));
        h3.addOperatingRoom(new OperatingRoom(7, 3, "Trauma OR-2", Arrays.asList(Patient.SurgeryType.CARDIAC, Patient.SurgeryType.GENERAL)));
        h3.addStaff(new Staff(4, "Dr. Williams", Staff.Role.SURGEON, 3, Arrays.asList(Patient.SurgeryType.NEUROLOGICAL, Patient.SurgeryType.ORTHOPEDIC, Patient.SurgeryType.CARDIAC, Patient.SurgeryType.GENERAL), 480, 1200)); // 8am-20pm
        hospitals.add(h3);

        return hospitals;
    }

    public static List<Patient> getPatients() {
        List<Patient> patients = new ArrayList<>();
        // ID, Name, Age, Priority, SurgeryType, Duration(m), ArrivalTime(m), preferredHospital, ICU
        patients.add(new Patient(1, "Alice Brown", 45, Patient.Priority.ELECTIVE, Patient.SurgeryType.GENERAL, 60, 480, 1, false));
        patients.add(new Patient(2, "Bob Smith", 62, Patient.Priority.URGENT, Patient.SurgeryType.CARDIAC, 180, 500, 1, true));
        patients.add(new Patient(3, "Charlie Davis", 28, Patient.Priority.EMERGENCY, Patient.SurgeryType.NEUROLOGICAL, 120, 510, 2, true)); // Preferred Hosp 2, but needs Neuro (might get routed!)
        patients.add(new Patient(4, "Diana Prince", 35, Patient.Priority.ELECTIVE, Patient.SurgeryType.ORTHOPEDIC, 90, 540, 2, false));
        patients.add(new Patient(5, "Evan Wright", 50, Patient.Priority.URGENT, Patient.SurgeryType.ENT, 45, 550, 1, false));
        patients.add(new Patient(6, "Fiona Ray", 71, Patient.Priority.EMERGENCY, Patient.SurgeryType.CARDIAC, 240, 560, 3, true));
        patients.add(new Patient(7, "George Hill", 19, Patient.Priority.URGENT, Patient.SurgeryType.ORTHOPEDIC, 75, 580, 2, false));
        patients.add(new Patient(8, "Hannah Lee", 44, Patient.Priority.ELECTIVE, Patient.SurgeryType.OPHTHALMIC, 30, 600, 2, false));
        patients.add(new Patient(9, "Ian Cook", 55, Patient.Priority.URGENT, Patient.SurgeryType.GENERAL, 105, 620, 1, false));
        patients.add(new Patient(10, "Julia Bell", 39, Patient.Priority.EMERGENCY, Patient.SurgeryType.NEUROLOGICAL, 150, 640, 3, true));
        patients.add(new Patient(11, "Kevin Ford", 60, Patient.Priority.ELECTIVE, Patient.SurgeryType.GENERAL, 50, 660, 1, false));
        patients.add(new Patient(12, "Lucy Kane", 22, Patient.Priority.EMERGENCY, Patient.SurgeryType.ORTHOPEDIC, 90, 700, 3, false));
        return patients;
    }
}
