package com.ihtsdo.snomed.service.refset;

import java.util.List;

import com.ihtsdo.snomed.exception.MemberNotFoundException;
import com.ihtsdo.snomed.model.refset.Member;
import com.ihtsdo.snomed.service.refset.RefsetService.SortOrder;

public interface MemberService {

    public abstract Member findByMemberPublicIdAndRefsetPublicId(String memberPublicId, String refsetPublicId) throws MemberNotFoundException;
    
    public abstract List<Member> findByRefsetPublicId(String refsetPublicId, String sortBy, SortOrder sortOrder);

    public abstract List<Member> findBySnapshotPublicId(String refsetPublicId, String snapshotPublicId, String sortBy, SortOrder sortOrder);    
}
