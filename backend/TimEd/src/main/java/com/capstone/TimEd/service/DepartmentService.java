package com.capstone.TimEd.service;
import com.capstone.TimEd.model.Department;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class DepartmentService {

    private static final String COLLECTION_NAME = "departments";

    public String createDepartment(Department department) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection(COLLECTION_NAME).document();
        department.setDepartmentId(docRef.getId());
        ApiFuture<WriteResult> future = docRef.set(department);
        return "Created at: " + future.get().getUpdateTime();
    }

    public Department getDepartment(String departmentId) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot document = db.collection(COLLECTION_NAME).document(departmentId).get().get();
        return document.exists() ? document.toObject(Department.class) : null;
    }

    public String updateDepartment(String departmentId, Department updatedDepartment) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        updatedDepartment.setDepartmentId(departmentId);
        ApiFuture<WriteResult> future = db.collection(COLLECTION_NAME).document(departmentId).set(updatedDepartment);
        return "Updated at: " + future.get().getUpdateTime();
    }

    public String deleteDepartment(String departmentId) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        
        try {
            // Delete the department document
            ApiFuture<WriteResult> future = db.collection(COLLECTION_NAME).document(departmentId).delete();
            future.get(); // Wait for deletion to complete
            
            // Update users who belong to this department
            CollectionReference usersRef = db.collection("users");
            ApiFuture<QuerySnapshot> querySnapshot = usersRef.whereEqualTo("departmentId", departmentId).get();
            List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
            
            // Batch update for users
            WriteBatch batch = db.batch();
            for (QueryDocumentSnapshot document : documents) {
                DocumentReference userRef = usersRef.document(document.getId());
                Map<String, Object> updates = new HashMap<>();
                updates.put("departmentId", null);
                batch.update(userRef, updates);
            }
            
            // Commit the batch
            batch.commit().get();
            
            return "Department with ID " + departmentId + " deleted successfully.";
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete department: " + e.getMessage());
        }
    }

    public List<Department> getAllDepartments() throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> future = db.collection(COLLECTION_NAME).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        List<Department> departmentList = new ArrayList<>();
        for (QueryDocumentSnapshot doc : documents) {
            departmentList.add(doc.toObject(Department.class));
        }
        return departmentList;
    }

    public Department getDepartmentByName(String departmentName) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> future = db.collection(COLLECTION_NAME)
                .whereEqualTo("name", departmentName)
                .limit(1)
                .get();
        
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        if (!documents.isEmpty()) {
            return documents.get(0).toObject(Department.class);
        }
        return null;
    }
}