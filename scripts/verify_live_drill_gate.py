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

import hashlib
import os
import re
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

def verify_live_drills_on_disk(repo_root: str) -> int:
    """
    Validates actual live drill documentation and evidence on disk.
    Ensures that no live drill improperly claims PASS or ACCEPTED without:
    1. A signed Owner Sign-off block
    2. An uncorrupted evidence manifest
    3. No NOT RUN steps if targeting main
    """
    print("\n" + "=" * 70)
    print("MobiDesk — On-Disk Live Drill Integrity Verification")
    print("=" * 70)

    drills_dir = os.path.join(repo_root, "docs", "live-drills")
    if not os.path.isdir(drills_dir):
        print(f"Notice: live-drills directory not found at {drills_dir}")
        return 0

    base_ref = os.environ.get("GITHUB_BASE_REF", "")
    is_pr_to_main = base_ref == "main" or "--pr-main" in sys.argv

    violations = []

    for item in sorted(os.listdir(drills_dir)):
        item_path = os.path.join(drills_dir, item)
        if not os.path.isdir(item_path) or item in ("TEMPLATE", ".git"):
            continue

        report_path = os.path.join(item_path, "EXECUTION_REPORT.md")
        checklist_path = os.path.join(item_path, "DEVICE_OR_ENVIRONMENT_CHECKLIST.md")
        manifest_path = os.path.join(item_path, "evidence", "manifest.sha256")
        evidence_dir = os.path.join(item_path, "evidence")

        if os.path.isfile(report_path):
            with open(report_path, "r", encoding="utf-8", errors="ignore") as fp:
                report_text = fp.read()

            # Check actual current status claims line by line
            is_pass_claimed = False
            is_accepted_claimed = False
            for line in report_text.splitlines():
                lower_line = line.lower()
                if "current live-drill status" in lower_line or "live_drill_status" in lower_line:
                    if "pass" in lower_line and not any(k in lower_line for k in ["not", "fail", "ready", "blocked", "awaiting"]):
                        is_pass_claimed = True
                if "owner sign-off status" in lower_line or "owner decision" in lower_line:
                    if "accepted" in lower_line and "unaccepted" not in lower_line:
                        is_accepted_claimed = True

            # Check sign-off block
            has_sign_off_block = "Owner Sign-off Block" in report_text or "OWNER SIGN-OFF" in report_text or "PROJECT OWNER LIVE-DRILL SIGN-OFF" in report_text
            # A valid signature must have alphanumeric characters beyond underscores or brackets
            signed_match = re.search(r"(?:Signature|Signed by):\s*([^\r\n]+)", report_text, re.IGNORECASE)
            is_signed = False
            if signed_match:
                sig_val = signed_match.group(1).strip().strip("_[]")
                if sig_val and not any(placeholder in sig_val.lower() for placeholder in ["awaiting", "pending", "none", "recorded"]):
                    is_signed = True

            if is_pass_claimed or is_accepted_claimed:
                if not (has_sign_off_block and is_signed):
                    violations.append(f"{item}: Claims PASS/ACCEPTED in EXECUTION_REPORT.md but lacks signed Owner Sign-off block.")

            # Validate manifest if present
            if os.path.isfile(manifest_path):
                with open(manifest_path, "r", encoding="utf-8") as fp:
                    for line in fp:
                        line = line.strip()
                        if not line or line.startswith("#"):
                            continue
                        parts = line.split(None, 1)
                        if len(parts) == 2:
                            expected_sha, fname = parts
                            fpath = os.path.join(evidence_dir, os.path.basename(fname))
                            if not os.path.isfile(fpath):
                                violations.append(f"{item}: Manifest entry missing on disk: {fname}")
                            else:
                                with open(fpath, "rb") as ef:
                                    actual_sha = hashlib.sha256(ef.read()).hexdigest()
                                if actual_sha.lower() != expected_sha.lower():
                                    violations.append(f"{item}: Manifest hash mismatch for {fname} (expected {expected_sha}, got {actual_sha})")

        # Validate checklist steps if PR targets main
        if is_pr_to_main and os.path.isfile(checklist_path):
            with open(checklist_path, "r", encoding="utf-8", errors="ignore") as fp:
                chk_text = fp.read()
            not_run_matches = re.findall(r"\|\s*NOT RUN\s*\|", chk_text, re.IGNORECASE)
            if not_run_matches:
                violations.append(f"{item}: PR targets main but DEVICE_OR_ENVIRONMENT_CHECKLIST.md contains {len(not_run_matches)} NOT RUN step(s).")

    if violations:
        print("\nGATE VIOLATIONS FOUND:")
        for v in violations:
            print(f"  ❌ {v}")
        return 1

    print("\nAll on-disk live drill checks passed. Zero premature acceptance claims found.")
    return 0


def test_real_kit_parser() -> int:
    """
    Validates that verify_live_drills_on_disk correctly parses a real kit structure,
    properly rejecting a blank sign-off block even if PASS or ACCEPTED is claimed.
    """
    import tempfile
    import shutil
    print("\n" + "=" * 70)
    print("Testing parser on real EXECUTION_REPORT copy with blank sign-off...")
    print("=" * 70)

    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    real_fp01 = os.path.join(repo_root, "docs", "live-drills", "FP-01")
    if not os.path.isdir(real_fp01):
        print("Notice: FP-01 live drill directory not found for parser test, skipping.")
        return 0

    with tempfile.TemporaryDirectory() as td:
        fake_drills = os.path.join(td, "docs", "live-drills", "FP-01")
        shutil.copytree(real_fp01, fake_drills)

        report_file = os.path.join(fake_drills, "EXECUTION_REPORT.md")
        with open(report_file, "r", encoding="utf-8") as f:
            content = f.read()

        # Injected premature PASS and ACCEPTED claims with unmodified blank sign-off
        tampered = re.sub(r"LIVE_DRILL_STATUS:\s*[A-Z_ ]+", "LIVE_DRILL_STATUS: PASS", content)
        tampered = re.sub(r"Owner\s*Sign-off\s*Status\s*:\s*[A-Z]+", "Owner Sign-off Status: ACCEPTED", tampered)
        with open(report_file, "w", encoding="utf-8") as f:
            f.write(tampered)

        rc = verify_live_drills_on_disk(td)
        if rc != 0:
            print("  --> PASS: Gate correctly rejected real kit copy claiming PASS with blank sign-off block.")
            return 0
        else:
            print("  --> FAIL: Gate failed to reject real kit copy with blank sign-off!")
            return 1


def main() -> int:
    # 1. Run mandatory test suite (including negative tests)
    test_rc = run_tests()
    if test_rc != 0:
        return test_rc

    # 2. Run real kit parser tests
    parser_rc = test_real_kit_parser()
    if parser_rc != 0:
        return parser_rc

    # 3. Verify repository on-disk live drill documents
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    disk_rc = verify_live_drills_on_disk(repo_root)
    return disk_rc


if __name__ == "__main__":
    sys.exit(main())
