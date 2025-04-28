package com.capstone.TimEd.dto;

import java.util.List;

public class Departmentdto {

    private String departmentId; // Firebase UID
    private String name;
    private String abbreviation;
    private int numberOfFaculty;
    private List<String> offeredPrograms;

    // Constructors
    public Departmentdto() {}

    public Departmentdto(String departmentId, String name, String abbreviation, int numberOfFaculty, List<String> offeredPrograms) {
        this.departmentId = departmentId;
        this.name = name;
        this.abbreviation = abbreviation;
        this.numberOfFaculty = numberOfFaculty;
        this.offeredPrograms = offeredPrograms;
    }

    // Getters and Setters
    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public int getNumberOfFaculty() {
        return numberOfFaculty;
    }

    public void setNumberOfFaculty(int numberOfFaculty) {
        this.numberOfFaculty = numberOfFaculty;
    }

    public List<String> getOfferedPrograms() {
        return offeredPrograms;
    }

    public void setOfferedPrograms(List<String> offeredPrograms) {
        this.offeredPrograms = offeredPrograms;
    }
}

