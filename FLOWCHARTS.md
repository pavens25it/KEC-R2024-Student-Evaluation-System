# Kongu Engineering College (Autonomous), Perundurai, Erode - 638 060
## Regulation 2024 (R2024) Student Academic & Continuous Evaluation System
### System Architecture & Workflow Flowcharts

---

## 1. Overall System Architecture & JDBC Flow

```mermaid
flowchart TD
    User([User / Faculty / Admin]) <-->|Console Inputs / Menu Selections| UI[MainApp.java\nConsole Interface]

    subgraph Business_and_Data_Access_Layer["Business & Data Access Layer (DAO)"]
        UI -->|Student & Course Actions| SDAO[StudentDAO.java]
        UI -->|Enrollment, Attendance & Evaluations| ADAO[AcademicDAO.java]
    end

    subgraph Database_Connection_Manager["Database Connection Manager"]
        SDAO -->|Get Connection| DBC[DBConnection.java]
        ADAO -->|Get Connection| DBC
        Props[(db.properties)] -.->|Loads Credentials| DBC
    end

    subgraph JDBC_Driver_Engine["JDBC Driver Engine"]
        DBC -->|PreparedStatement / Query| Driver[MySQL Connector/J 9.1.0\ncom.mysql.cj.jdbc.Driver]
    end

    subgraph MySQL_Database["MySQL 8.0 Database (kec_eval_db)"]
        Driver <-->|TCP Socket Port 3306| DB[(kec_eval_db)]
        DB --- T1[(students)]
        DB --- T2[(courses)]
        DB --- T3[(enrollments)]
        DB --- T4[(assessment_marks)]
    end

    ADAO -->|Generates Formatted Reports| Report[Semester Mark Sheet\nDefaulter Report\nDepartment Summary]
    Report -->|Displays on Screen| User
```

---

## 2. Attendance Gating & ESE Eligibility Flowchart

```mermaid
flowchart TD
    Start([Start: Attendance Check]) --> CalcAtt[Calculate Attendance % = Attended / Held * 100]

    CalcAtt --> Check75{Attendance >= 75%?}
    Check75 -- Yes --> Eligible[Status: ELIGIBLE\nPermitted to appear in ESE]
    
    Check75 -- No --> Check65{Attendance >= 65%?}
    
    Check65 -- Yes --> CondCheck{Medical Condonation\nApproved by Principal?}
    CondCheck -- Yes --> Condoned[Status: CONDONED\nPermitted to appear in ESE]
    CondCheck -- No --> BarredSA1[Status: SA Shortage of Attendance\nBarred from ESE\nGrade Point: 0]
    
    Check65 -- No --> BarredSA2[Status: SA Shortage of Attendance\nStrict Attendance Defaulter\nBarred from ESE\nGrade Point: 0]

    Eligible --> ProceedExam([Proceed to ESE Marks Entry])
    Condoned --> ProceedExam
    BarredSA1 --> FinalSA([Final Grade: SA, Result: FAIL])
    BarredSA2 --> FinalSA
```

---

## 3. Continuous Assessment & Course Evaluation Flowchart (KEC R2024)

```mermaid
flowchart TD
    StartEval([Start Course Evaluation]) --> AttCheck{Is Student Eligible\nfor ESE?}

    AttCheck -- No --> AssignSA[Grade: SA\nGrade Point: 0\nResult: FAIL SA]
    
    AttCheck -- Yes --> CheckAbsent{Was Student Absent\nfor ESE?}
    CheckAbsent -- Yes --> AssignAB[Grade: AB\nGrade Point: 0\nResult: ABSENT]

    CheckAbsent -- No --> CheckMarks{Are CIA 1, CIA 2,\nAssignment and ESE\nentered?}
    CheckMarks -- Incomplete --> AssignPending[Status: PENDING\nGrade: --, GP: --\nResult: PENDING]

    CheckMarks -- Complete --> CalcInternal["Calculate 100-Scale Internal:\nCIA_100 = CIA1*0.40 + CIA2*0.40 + Assignment*0.20"]

    CalcInternal --> CheckType{Course Type?}

    CheckType -- THEORY --> ScaleTheory["CIA_W = CIA_100 * 0.40 (Max 40)\nESE_W = (ESE/100) * 60 (Max 60)\nMin ESE Required: 27 / 60"]
    CheckType -- PRACTICAL --> ScalePrac["CIA_W = CIA_100 * 0.60 (Max 60)\nESE_W = (ESE/100) * 40 (Max 40)\nMin ESE Required: 20 / 40"]
    CheckType -- EMBEDDED --> ScaleEmbed["CIA_W = CIA_100 * 0.50 (Max 50)\nESE_W = (ESE/100) * 50 (Max 50)\nMin ESE Required: 22.5 / 50"]

    ScaleTheory --> SumMarks[Total Marks = CIA_W + ESE_W]
    ScalePrac --> SumMarks
    ScaleEmbed --> SumMarks

    SumMarks --> CheckPass{ESE_W >= Min ESE\nAND\nTotal Marks >= 50?}

    CheckPass -- No --> GradeRA[Grade: RA\nGrade Point: 0\nResult: RE-APPEAR RA]
    
    CheckPass -- Yes --> GradeScale{Total Marks Range}
    GradeScale -- 91 to 100 --> GradeO[Grade: O | GP: 10]
    GradeScale -- 81 to 90 --> GradeA1[Grade: A+ | GP: 9]
    GradeScale -- 71 to 80 --> GradeA[Grade: A | GP: 8]
    GradeScale -- 61 to 70 --> GradeB1[Grade: B+ | GP: 7]
    GradeScale -- 50 to 60 --> GradeB[Grade: B | GP: 6]

    GradeO --> EndPass([Result: PASS])
    GradeA1 --> EndPass
    GradeA --> EndPass
    GradeB1 --> EndPass
    GradeB --> EndPass
    GradeRA --> EndFail([Result: FAIL])
    AssignSA --> EndFail
    AssignAB --> EndFail
    AssignPending --> EndPending([Result: PENDING])
```

---

## 4. Semester Mark Sheet & SGPA Calculation Flowchart

```mermaid
flowchart TD
    StartSGPA([Request Semester Mark Sheet]) --> FetchData[Fetch Student Profile & All Enrolled Courses for Semester]

    FetchData --> LoopCourses[Evaluate Each Enrolled Course]

    LoopCourses --> CheckPending{Are any enrolled\ncourses PENDING?}

    CheckPending -- Yes --> SGPAPending[SGPA: PENDING\nEquivalent %: PENDING\nNote: Awaiting completion of all course evaluations]

    CheckPending -- No --> CalcSGPA["Calculate Final SGPA:\nSGPA = Sum(Credits * Grade_Point) / Sum(Credits)\nEquivalent Percentage = SGPA * 10"]

    SGPAPending --> PrintSheet[Format Institutional Mark Sheet Table\nWidth: 90 columns]
    CalcSGPA --> PrintSheet

    PrintSheet --> Display([Display on Console Screen])
```

---

## 5. Main Application Menu Navigation Flowchart

```mermaid
flowchart TD
    Start([Launch MainApp]) --> DBCheck{Test DB Connection\nvia DBConnection}

    DBCheck -- Success --> ShowMenu[Display Main Menu: Options 0 to 13]
    DBCheck -- Warning --> Warn[Display Warning & Prompt Config] --> ShowMenu

    ShowMenu --> Choice{Select Option}

    Choice -- 1 --> Opt1[Add Student] --> ShowMenu
    Choice -- 2 --> Opt2[View Students] --> ShowMenu
    Choice -- 3 --> Opt3[Add Course] --> ShowMenu
    Choice -- 4 --> Opt4[View Courses] --> ShowMenu
    Choice -- 5 --> Opt5[Enroll Student & Attendance] --> ShowMenu
    Choice -- 6 --> Opt6[Update Attendance] --> ShowMenu
    Choice -- 7 --> Opt7[Enter / Update CIA 1 Phase 1] --> ShowMenu
    Choice -- 8 --> Opt8[Enter / Update CIA 2 Phase 1] --> ShowMenu
    Choice -- 9 --> Opt9[Enter / Update Assignment Phase 2] --> ShowMenu
    Choice -- 10 --> Opt10[Enter ESE Marks or Mark AB Phase 3] --> ShowMenu
    Choice -- 11 --> Opt11[Generate Semester Mark Sheet & SGPA] --> ShowMenu
    Choice -- 12 --> Opt12[Generate Defaulter Report] --> ShowMenu
    Choice -- 13 --> Opt13[Department Performance Summary] --> ShowMenu
    Choice -- 0 --> ExitApp([Exit Application])
```
