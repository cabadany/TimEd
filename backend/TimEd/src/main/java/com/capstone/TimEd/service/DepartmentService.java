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

    public String deleteDepartment(String departmentId) {
        Firestore db = FirestoreClient.getFirestore();
        db.collection(COLLECTION_NAME).document(departmentId).delete();
        return "Department with ID " + departmentId + " deleted.";
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
}