package com.mobidesk.android

import com.mobidesk.android.data.StudentProfile
import com.mobidesk.android.data.StudentWorkspaceRepository
import com.mobidesk.android.data.StudentWorkspaceResult
import com.mobidesk.android.data.VmSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentAssignmentResolutionTest {

    private val sampleProfile = StudentProfile(
        id = "student-uuid-1",
        name = "Alex Rivera",
        college = "College of Engineering"
    )

    @Test
    fun testNullProfile_returnsAccountNotProvisioned() {
        val result = StudentWorkspaceRepository.resolve(null, emptyList())
        assertEquals(StudentWorkspaceResult.AccountNotProvisioned, result)
    }

    @Test
    fun testZeroAssignedVms_returnsNoAssignment() {
        val result = StudentWorkspaceRepository.resolve(sampleProfile, emptyList())
        assertTrue(result is StudentWorkspaceResult.NoAssignment)
        assertEquals(sampleProfile, (result as StudentWorkspaceResult.NoAssignment).profile)
    }

    @Test
    fun testSingleValidVm_returnsAssignedWithDerivedTag() {
        val vm = VmSummary(id = "vm-uuid-99", name = "CAD Workstation 01")
        val result = StudentWorkspaceRepository.resolve(sampleProfile, listOf(vm))
        assertTrue(result is StudentWorkspaceResult.Assigned)
        val assigned = result as StudentWorkspaceResult.Assigned
        assertEquals(sampleProfile, assigned.profile)
        assertEquals("CAD Workstation 01", assigned.assignedWorkspace.displayLabel)
        assertEquals("asg_tag_vm-uuid-99", assigned.assignedWorkspace.assignmentTag)
    }

    @Test
    fun testBlankVmId_failsClosedAsAssignmentInvalid() {
        val vm = VmSummary(id = "", name = "CAD Workstation 01")
        val result = StudentWorkspaceRepository.resolve(sampleProfile, listOf(vm))
        assertTrue("Blank VM id must fail closed as AssignmentInvalid", result is StudentWorkspaceResult.AssignmentInvalid)
        val invalid = result as StudentWorkspaceResult.AssignmentInvalid
        assertTrue(invalid.reason.contains("blank", ignoreCase = true))
    }

    @Test
    fun testBlankVmName_failsClosedAsAssignmentInvalid() {
        val vm = VmSummary(id = "vm-uuid-99", name = "   ")
        val result = StudentWorkspaceRepository.resolve(sampleProfile, listOf(vm))
        assertTrue("Blank VM name must fail closed as AssignmentInvalid", result is StudentWorkspaceResult.AssignmentInvalid)
        val invalid = result as StudentWorkspaceResult.AssignmentInvalid
        assertTrue(invalid.reason.contains("blank", ignoreCase = true))
    }

    @Test
    fun testMultipleAssignedVms_failsClosedAsAssignmentConflict() {
        val vm1 = VmSummary(id = "vm-1", name = "CAD Workstation 01")
        val vm2 = VmSummary(id = "vm-2", name = "CAD Workstation 02")
        val result = StudentWorkspaceRepository.resolve(sampleProfile, listOf(vm1, vm2))
        assertTrue(result is StudentWorkspaceResult.AssignmentInvalid)
        val invalid = result as StudentWorkspaceResult.AssignmentInvalid
        assertTrue(invalid.reason.contains("Multiple", ignoreCase = true))
    }
}
