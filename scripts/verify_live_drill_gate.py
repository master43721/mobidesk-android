#!/usr/bin/env python3
"""
MobiDesk — Mandatory Live Drill Gate Process Verification Test
Governance: docs/MANDATORY_LIVE_DRILL_POLICY.md

Demonstrates the 4 mandatory gate conditions:
1. Simulated FP with passing automated tests but no live drill remains NOT ACCEPTED.
2. Simulated FP with a prepared APK but no Owner interaction remains NOT ACCEPTED.
3. Simulated FP with all live scenarios passed but no Owner sign-off remains NOT ACCEPTED.
4. Simulated FP with required evidence and Owner sign-off may become ACCEPTED.
"""

import sys
from dataclasses import dataclass, field
from enum import Enum
from typing import Optional, List

class LiveDrillStatus(Enum):
    NOT_REQUESTED = "NOT REQUESTED"
    NOT_READY = "NOT READY"
    READY_FOR_OWNER = "READY FOR OWNER"
    IN_PROGRESS = "IN PROGRESS"
    PASS = "PASS"
    FAIL = "FAIL"
    BLOCKED = "BLOCKED"
    NOT_RUN = "NOT RUN"

class OwnerDecision(Enum):
    ACCEPTED = "ACCEPTED"
    REJECTED = "REJECTED"
    NONE = "NONE"

@dataclass
class FeaturePointGateState:
    fp_id: str
    implementation_passed: bool = False
    automated_tests_passed: bool = False
    build_verified: bool = False
    independent_review_passed: bool = False
    live_drill_status: LiveDrillStatus = LiveDrillStatus.NOT_REQUESTED
    evidence_directory_complete: bool = False
    owner_decision: OwnerDecision = OwnerDecision.NONE
    defects: List[str] = field(default_factory=list)

    def evaluate_acceptance(self) -> str:
        """
        Terminal Acceptance Invariant:
        FP Completion = (Implementation == PASS)
                      AND (Automated Tests == PASS)
                      AND (Build Verification == PASS)
                      AND (Independent Review == PASS)
                      AND (LIVE_DRILL_STATUS == PASS)
                      AND (Project Owner Decision == ACCEPTED)
        """
        if not self.implementation_passed:
            return "NOT ACCEPTED (Implementation incomplete)"
        if not self.automated_tests_passed:
            return "NOT ACCEPTED (Automated tests failing or incomplete)"
        if not self.build_verified:
            return "NOT ACCEPTED (Build verification incomplete)"
        if not self.independent_review_passed:
            return "NOT ACCEPTED (Independent review incomplete or changes requested)"

        # Live Drill Gate Enforcements
        if self.live_drill_status != LiveDrillStatus.PASS:
            return f"NOT ACCEPTED (Live drill gate not passed; status={self.live_drill_status.value})"

        if not self.evidence_directory_complete:
            return "NOT ACCEPTED (Live drill evidence directory missing or incomplete)"

        if self.owner_decision != OwnerDecision.ACCEPTED:
            return f"NOT ACCEPTED (Owner decision is {self.owner_decision.value}; written sign-off required)"

        return "ACCEPTED"


def run_tests() -> int:
    print("=" * 70)
    print("MobiDesk — Mandatory Live Drill Gate Verification Suite")
    print("=" * 70)

    passed_cases = 0
    total_cases = 4

    # -------------------------------------------------------------
    # Case 1: Passing automated tests but no live drill
    # -------------------------------------------------------------
    print("\n[TEST 1] Simulated FP with passing automated tests but no live drill...")
    fp1 = FeaturePointGateState(
        fp_id="SIM-FP-01",
        implementation_passed=True,
        automated_tests_passed=True,
        build_verified=True,
        independent_review_passed=True,
        live_drill_status=LiveDrillStatus.NOT_REQUESTED,
        evidence_directory_complete=False,
        owner_decision=OwnerDecision.NONE
    )
    result1 = fp1.evaluate_acceptance()
    print(f"  Status: LIVE_DRILL_STATUS={fp1.live_drill_status.value}, OwnerDecision={fp1.owner_decision.value}")
    print(f"  Evaluation Result: {result1}")
    if "NOT ACCEPTED" in result1:
        print("  --> PASS: FP correctly remains NOT ACCEPTED without live drill.")
        passed_cases += 1
    else:
        print("  --> FAIL: FP was incorrectly marked accepted!")

    # -------------------------------------------------------------
    # Case 2: Prepared APK but no Owner interaction
    # -------------------------------------------------------------
    print("\n[TEST 2] Simulated FP with prepared APK but no Owner interaction...")
    fp2 = FeaturePointGateState(
        fp_id="SIM-FP-02",
        implementation_passed=True,
        automated_tests_passed=True,
        build_verified=True,
        independent_review_passed=True,
        live_drill_status=LiveDrillStatus.READY_FOR_OWNER,
        evidence_directory_complete=False,
        owner_decision=OwnerDecision.NONE
    )
    result2 = fp2.evaluate_acceptance()
    print(f"  Status: LIVE_DRILL_STATUS={fp2.live_drill_status.value}, OwnerDecision={fp2.owner_decision.value}")
    print(f"  Evaluation Result: {result2}")
    if "NOT ACCEPTED" in result2:
        print("  --> PASS: FP correctly remains NOT ACCEPTED when APK is ready but unexercised.")
        passed_cases += 1
    else:
        print("  --> FAIL: FP was incorrectly marked accepted!")

    # -------------------------------------------------------------
    # Case 3: All live scenarios passed but no Owner sign-off
    # -------------------------------------------------------------
    print("\n[TEST 3] Simulated FP with all live scenarios passed but no Owner sign-off...")
    fp3 = FeaturePointGateState(
        fp_id="SIM-FP-03",
        implementation_passed=True,
        automated_tests_passed=True,
        build_verified=True,
        independent_review_passed=True,
        live_drill_status=LiveDrillStatus.PASS,
        evidence_directory_complete=True,
        owner_decision=OwnerDecision.NONE
    )
    result3 = fp3.evaluate_acceptance()
    print(f"  Status: LIVE_DRILL_STATUS={fp3.live_drill_status.value}, OwnerDecision={fp3.owner_decision.value}")
    print(f"  Evaluation Result: {result3}")
    if "NOT ACCEPTED" in result3:
        print("  --> PASS: FP correctly remains NOT ACCEPTED when Owner has not given written sign-off.")
        passed_cases += 1
    else:
        print("  --> FAIL: FP was incorrectly marked accepted without Owner sign-off!")

    # -------------------------------------------------------------
    # Case 4: Required evidence and Owner sign-off present
    # -------------------------------------------------------------
    print("\n[TEST 4] Simulated FP with required evidence and Owner sign-off...")
    fp4 = FeaturePointGateState(
        fp_id="SIM-FP-04",
        implementation_passed=True,
        automated_tests_passed=True,
        build_verified=True,
        independent_review_passed=True,
        live_drill_status=LiveDrillStatus.PASS,
        evidence_directory_complete=True,
        owner_decision=OwnerDecision.ACCEPTED
    )
    result4 = fp4.evaluate_acceptance()
    print(f"  Status: LIVE_DRILL_STATUS={fp4.live_drill_status.value}, OwnerDecision={fp4.owner_decision.value}")
    print(f"  Evaluation Result: {result4}")
    if result4 == "ACCEPTED":
        print("  --> PASS: FP correctly transitions to ACCEPTED with passing drill and Owner sign-off.")
        passed_cases += 1
    else:
        print(f"  --> FAIL: Expected ACCEPTED, got {result4}")

    # -------------------------------------------------------------
    # Summary
    # -------------------------------------------------------------
    print("\n" + "=" * 70)
    print(f"Process Verification Summary: {passed_cases}/{total_cases} tests passed.")
    print("=" * 70)

    return 0 if passed_cases == total_cases else 1

if __name__ == "__main__":
    sys.exit(run_tests())
