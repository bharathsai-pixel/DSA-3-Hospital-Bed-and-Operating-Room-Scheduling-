package scheduling;

import algorithms.MinCostMaxFlow;
import algorithms.MinCostMaxFlow.Edge;
import models.Bed;
import models.Hospital;
import models.Patient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Routes patients by maximum flow, then prefers their requested hospital and shorter travel distance. */
public class HospitalRouter {
    private static final int PREFERENCE_PENALTY = 6_000_000;
    private static final int ICU_PRESERVATION_PENALTY = 3_000_000;

    public static Map<Integer, List<Patient>> routePatients(List<Patient> patients, List<Hospital> hospitals) {
        return routePatients(patients, hospitals, 480);
    }

    public static Map<Integer, List<Patient>> routePatients(List<Patient> patients, List<Hospital> hospitals,
                                                            int routingTime) {
        Map<Integer, List<Patient>> result = new HashMap<>();
        for (Hospital hospital : hospitals) result.put(hospital.getId(), new ArrayList<>());
        if (patients.isEmpty() || hospitals.isEmpty()) return result;

        int source = 0;
        int patientStart = 1;
        int laneStart = patientStart + patients.size();
        int sink = laneStart + hospitals.size() * 2;
        MinCostMaxFlow flow = new MinCostMaxFlow(sink + 1);
        Edge[][][] patientHospitalEdges = new Edge[patients.size()][hospitals.size()][2]; // general, ICU

        for (int i = 0; i < patients.size(); i++) {
            Patient patient = patients.get(i);
            flow.addEdge(source, patientStart + i, 1, 0);
            for (int j = 0; j < hospitals.size(); j++) {
                Hospital hospital = hospitals.get(j);
                if (!hospital.canPerformSurgery(patient.getSurgeryType())
                        || !hospital.hasQualifiedSurgeon(patient.getSurgeryType())) continue;
                int preferenceCost = patient.getPreferredHospitalId() == hospital.getId() ? 0 : PREFERENCE_PENALTY;
                int travelCost = (int) Math.round(hospital.distanceFrom(patient.getLatitude(), patient.getLongitude()) * 100.0);
                int baseCost = preferenceCost + travelCost;
                int generalNode = laneStart + j * 2;
                int icuNode = generalNode + 1;
                if (!patient.requiresICU() && availableBeds(hospital, Bed.BedType.GENERAL, routingTime) > 0) {
                    patientHospitalEdges[i][j][0] = flow.addEdge(patientStart + i, generalNode, 1, baseCost);
                }
                if (availableBeds(hospital, Bed.BedType.ICU, routingTime) > 0) {
                    int icuCost = patient.requiresICU() ? baseCost : baseCost + ICU_PRESERVATION_PENALTY;
                    patientHospitalEdges[i][j][1] = flow.addEdge(patientStart + i, icuNode, 1, icuCost);
                }
            }
        }

        for (int j = 0; j < hospitals.size(); j++) {
            Hospital hospital = hospitals.get(j);
            flow.addEdge(laneStart + j * 2, sink, availableBeds(hospital, Bed.BedType.GENERAL, routingTime), 0);
            flow.addEdge(laneStart + j * 2 + 1, sink, availableBeds(hospital, Bed.BedType.ICU, routingTime), 0);
        }
        flow.solve(source, sink);

        for (int i = 0; i < patients.size(); i++) {
            for (int j = 0; j < hospitals.size(); j++) {
                Edge general = patientHospitalEdges[i][j][0];
                Edge icu = patientHospitalEdges[i][j][1];
                if ((general != null && general.flow() > 0) || (icu != null && icu.flow() > 0)) {
                    Patient patient = patients.get(i);
                    Hospital hospital = hospitals.get(j);
                    patient.setAssignedHospitalId(hospital.getId());
                    result.get(hospital.getId()).add(patient);
                    break;
                }
            }
        }
        return result;
    }

    private static int availableBeds(Hospital hospital, Bed.BedType type, int time) {
        int count = 0;
        for (Bed bed : hospital.getBeds()) if (bed.getType() == type && bed.isAvailableAt(time)) count++;
        return count;
    }
}
