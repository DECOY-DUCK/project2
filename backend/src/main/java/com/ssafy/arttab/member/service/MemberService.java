package com.ssafy.arttab.member.service;

import com.ssafy.arttab.artwork.ArtworkRepository;
import com.ssafy.arttab.config.JWTUtil;
import com.ssafy.arttab.exception.authorization.NoauthorizedMemberException;
import com.ssafy.arttab.exception.member.DuplicateException;
import com.ssafy.arttab.exception.member.NoSuchMemberException;
import com.ssafy.arttab.exception.member.PasswordMismatchException;
import com.ssafy.arttab.follow.FollowRepository;
import com.ssafy.arttab.member.domain.MailAuth;
import com.ssafy.arttab.member.domain.Member;
import com.ssafy.arttab.member.dto.LoginEmail;
import com.ssafy.arttab.member.dto.User;
import com.ssafy.arttab.member.dto.request.AuthNumCheckRequest;
import com.ssafy.arttab.member.dto.request.IntroUpdateRequest;
import com.ssafy.arttab.member.dto.request.MemberSaveRequest;
import com.ssafy.arttab.member.dto.request.PasswordUpdateRequest;
import com.ssafy.arttab.member.dto.response.MemberInfoResponse;
import com.ssafy.arttab.member.dto.response.ProfileInfoResponse;
import com.ssafy.arttab.member.repository.MailAuthRepogitory;
import com.ssafy.arttab.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

/**
 * @packageName : com.ssafy.arttab.member
 * @fileName : MemberService
 * @date : 2022-02-03
 * @language : JAVA
 * @classification :
 * @time_limit : 2sec
 * @required_time : 00:40 ~ 01:22
 * @submissions : 1
 * @description :
 **/

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {
    private final JWTUtil jwtUtil;
    private final MemberRepository memberRepository;
    private final MailSendService mailSendService;
    private final MailAuthRepogitory mailAuthRepogitory;
    private final FollowRepository followRepository;
    private final ArtworkRepository artworkRepository;

//    @Value("${access.url.artworks}")
//    private String artworkImgUrl;

    @Value("${access.url.profiles}")
    private String profileImgUrl;

    @Value("${access.url.profileDefault}")
    private String profileDefaultImgUrl;

    /**
     * ?????? ??????
     * @param memberSaveRequest
     * @return
     */
    @Transactional
    public boolean saveMember(final MemberSaveRequest memberSaveRequest){
        //????????????
        MemberEmailCheck(memberSaveRequest.getEmail());


        //???????????? ?????????
        var password = BCrypt.hashpw(memberSaveRequest.getPassword(),BCrypt.gensalt());

        // ????????? ?????? ?????? ???????????? ??????
        String defaultSaveFolder=System.getProperty("user.dir")+File.separator+"src"+File.separator+"main"+File.separator+"resources"+File.separator+"static"+File.separator+"default.jpg";

        Member member = Member.builder()
                .email(memberSaveRequest.getEmail())
                .password(password)
                .saveFolder(defaultSaveFolder)
                .saveFilename("default.jpg")
                .build();

        try{
            memberRepository.save(member);
            //?????? ?????? ?????????
            SendNumtoEmail(member.getEmail());
            return true;
        }catch (Exception e){
            return false;
        }



    }

    /**
     * ????????? ?????? ??????
     * @param nickname
     * @return
     */
    @Transactional(readOnly = true)
    public void MemberIdCheck(final String nickname){
        if(memberRepository.existsMembersByNickname(nickname)){
            throw new DuplicateException();
        }

    }

    /**
     * ????????? ????????????
     * @param email
     * @return
     */
    @Transactional(readOnly = true)
    public void MemberEmailCheck(final String email){
        if(memberRepository.existsMembersByEmail(email)){
            throw new DuplicateException();
        }

    }

    /**
     * ???????????? ???????????? ?????????
     * @param email
     * @return
     */
    public void SendNumtoEmail(final String email){
        // ???????????? ??????
        final String pwd = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 10);


        // DB ??????
        Member member = memberRepository.findMemberByEmail(email)
                .orElseThrow(NoSuchMemberException::new);

        var mailAuth = mailAuthRepogitory.findById(member.getId());


        // DB??? ????????? ??????, ????????? ??????
        mailAuth.ifPresentOrElse(selectmailAuth ->{
                    selectmailAuth.setId(pwd);
                    mailAuthRepogitory.save(selectmailAuth);
                },() -> {
            var mailAuth1 = MailAuth.builder()
                    .id(pwd)
                    .memNo(member.getId())
                    .build();
            mailAuthRepogitory.save(mailAuth1);}


        );

        // ??????????????????
        StringBuilder cntnt = new StringBuilder();
        cntnt.append("?????? ????????? ")
                .append(pwd)
                .append( "?????????");
        mailSendService.sendEmail(email, "??????????????????  ??????????????? ??????????????? ????????? ????????????", cntnt.toString());
    }
    public void selectMailAuthId(final AuthNumCheckRequest authNumCheckRequest){
        //???????????? Id??????
        var member = memberRepository.findByEmail(authNumCheckRequest.getEmail())
                .orElseThrow(NoSuchMemberException::new);

        var mailAuth = mailAuthRepogitory.findById(member.getId())
                .orElseThrow(NoSuchMemberException::new);

        //???????????? ????????? ?????? ????????????
       if(mailAuth.getId().equals(authNumCheckRequest.getId())) {
           member.updateAuth();
           mailAuthRepogitory.delete(mailAuth);
       }else{
           throw new PasswordMismatchException();
       }
    }

    /**
     * ?????? ?????????
     * @param user
     */
    public String login(final User user){
        Member member = memberRepository.findByEmail(user.getEmail())
                .orElseThrow(NoSuchMemberException::new);

        if(member.getAuth()!=1){
            throw new NoauthorizedMemberException();
        }
        if (!BCrypt.checkpw(user.getPassword(), member.getPassword())) {
            throw new PasswordMismatchException("passwordMismatch");
        }
        //?????? ??????
        HashMap<String, Object> payload = new HashMap();
        payload.put("Id",member.getId());
        return jwtUtil.createToken(payload);
    }
    /**
     * ????????? ??????
     * @param loginEmail
     * @param nickname
     */
    @Transactional
    public void addNickname(final LoginEmail loginEmail,final String nickname ){
        var member = memberRepository.findByEmail(loginEmail.getEmail())
                .orElseThrow(NoSuchMemberException::new);

        member.updateNickname(nickname);
    }
    /**
     * ?????? ??????
     * @param loginEmail
     * @return
     */
    @Transactional(readOnly = true)
    public MemberInfoResponse getMemberInfo(final LoginEmail loginEmail){
        var member = memberRepository.findByEmail(loginEmail.getEmail())
                .orElseThrow(NoSuchMemberException::new);
        var memberInfoResponse = MemberInfoResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .intro(member.getIntro())
                .build();
        return memberInfoResponse;
    }

    /**
     * ???????????? ??????
     * @param
     * @param passwordUpdateRequest
     */
    @Transactional
    public void updatePassword(final PasswordUpdateRequest passwordUpdateRequest){
        var member = memberRepository.findByEmail(passwordUpdateRequest.getLoginEmail())
                .orElseThrow(NoSuchMemberException::new);
        if(BCrypt.checkpw(passwordUpdateRequest.getPassword(),member.getPassword())){
            member.updatepassword(BCrypt.hashpw(passwordUpdateRequest.getNewPassword(),BCrypt.gensalt()));
        }else{
            throw new PasswordMismatchException();
        }

    }

    /**
     * ???????????? ??????
     * @param email
     * @param
     */
    public void findPassword(final String email){
        var member = memberRepository.findByEmail(email)
                .orElseThrow(NoSuchMemberException::new);

        // ???????????? ??????
        final String pwd = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 10);

        // DB ??????
        member.updatepassword(pwd);

        //????????? ?????????
        StringBuilder cntnt = new StringBuilder();
        cntnt.append("?????? ??????????????? ")
                .append(pwd)
                .append( "?????????");
        mailSendService.sendEmail(email, "???????????????.Art Tab?????????." +member.getNickname()+"?????? ?????? ???????????? ?????????.", cntnt.toString());

    }

   /**
     * ????????? ??????
     * @param loginEmail
     * @param memberUpdateRequest
     */
    @Transactional
    public void updateMember(final LoginEmail loginEmail,final IntroUpdateRequest memberUpdateRequest){
        var member = memberRepository.findByEmail(loginEmail.getEmail())
                .orElseThrow(NoSuchMemberException::new);

        member.updateIntro(memberUpdateRequest.getIntro());
    }

    /**
     * ?????? ??????
     * @param loginEmail
     */
    @Transactional
    public void deleteMember(final LoginEmail loginEmail){
        var member = memberRepository.findByEmail(loginEmail.getEmail())
                .orElseThrow(NoSuchMemberException::new);

        memberRepository.delete(member);
    }

    // ?????? ???????????? ????????? ????????? ???????????? saveFolder ??????
    public String getParentFolder(String email){

        var member=memberRepository.findByEmail(email).orElseThrow();
        return member.getSaveFolder();

    }

    // saveFolder ??????: ???????????? ???????????? ????????? ?????? ??????
    @Transactional
    public void updateSaveFolder(final LoginEmail loginEmail, String saveFolder){
        var member=memberRepository.findByEmail(loginEmail.getEmail()).orElseThrow();
        member.updateSaveFolder(saveFolder);
    }

    // ????????? ????????? ????????????
    public ProfileInfoResponse getProfileInfo(Long loginId, Long profileMemberId){

        Long loginMember=memberRepository.findById(loginId).get().getId(); // ???????????? ?????? ?????????
        Long profileMember=memberRepository.findById(profileMemberId).get().getId(); // ???????????? ?????? ?????????

        String isFollow = "FALSE";
        if(loginMember==profileMember) { // ???????????? ???????????? ?????? ????????? ??????????????? ??? ???
            isFollow="ME";
        }
        else {
            int followOrNot = followRepository.isFollow(loginMember, profileMember);
            if (followOrNot != 0) { // ????????? ????????? ???
                isFollow = "TRUE";
            }
        }

        String defaultProfileImgUrl=System.getProperty("user.dir")+File.separator+"src"+File.separator+"main"+File.separator+"resources"+File.separator+"static"+File.separator+"default.jpg";
        String profileImg=memberRepository.findById(profileMemberId).get().getSaveFolder();
        String profileImageUrl=profileDefaultImgUrl+memberRepository.findById(profileMemberId).get().getSaveFilename(); // ?????? ????????? ????????? ???
        if(!profileImg.equals(defaultProfileImgUrl)){ // ????????? ???????????? ?????? ????????? ????????? ????????? ?????? ???
            profileImageUrl=profileImgUrl+memberRepository.findById(profileMemberId).get().getSaveFilename();
        }

        ProfileInfoResponse response = ProfileInfoResponse.builder()
                .nickname(memberRepository.findById(profileMemberId).get().getNickname())
                .isFollow(isFollow)
                .followedNum(followRepository.findAllFollowedCnt(profileMember))
                .followingNum(followRepository.findAllFollowingCnt(profileMember))
                .artworkNum(artworkRepository.findNumByMemberId(profileMember))
                .email(memberRepository.findById(profileMemberId).get().getEmail())
                .profileImageUrl(profileImageUrl)
                .build();

        return response;
    }

    public void updateSaveFilename(LoginEmail loginEmail, String saveFileName) {
        var member=memberRepository.findByEmail(loginEmail.getEmail()).orElseThrow();
        member.updateSaveFileName(saveFileName);
    }
}
