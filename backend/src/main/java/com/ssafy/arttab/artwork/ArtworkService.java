package com.ssafy.arttab.artwork;

import com.ssafy.arttab.artwork.dto.*;
import com.ssafy.arttab.follow.Follow;
import com.ssafy.arttab.follow.FollowRepository;
import com.ssafy.arttab.like.Likes;
import com.ssafy.arttab.like.LikesRepository;
import com.ssafy.arttab.member.domain.Member;
import com.ssafy.arttab.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ArtworkService {

    private final ArtworkRepository artworkRepository;
    private final MemberRepository memberRepository;
    private final LikesRepository likeRepository;
    private final FollowRepository followRepository;

    @Value("${access.url.artworks}")
    private String artworkImgUrl;

    @Value("${access.url.profiles}")
    private String profileImgUrl;

    @Transactional
    public List<ArtworkListResponseDto> getArtworkList(int page){
        Page<Artwork> pageResult = artworkRepository.findAll(PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "id")));
        List<ArtworkListResponseDto> result = new ArrayList<>();

        for(Artwork artwork: pageResult){
            ArtworkListResponseDto response = ArtworkListResponseDto.builder()
                    .memberId(artwork.getWriter().getId())
                    .memberNickname(artwork.getWriter().getNickname())
                    .artworkId(artwork.getId())
                    .artworkTitle(artwork.getTitle())
                    .artworkRegdate(artwork.getRegdate())
                    .saveFileName(artwork.getSaveFileName())
                    .saveFolder(artwork.getSaveFolder())
                    .imageUrl(artworkImgUrl+artwork.getSaveFileName())
                    .build();

            result.add(response);
        }

        return result;
    }

    @Transactional
    public boolean save(ArtworkFileDto artworkDto) {
    // artworkDto ????????? artwork??? ??????
        Artwork artwork = artworkRepository.save(
                Artwork.builder()
                        .writer(memberRepository.findById(artworkDto.getWriterId()).get())
                        .galleryItemList(null)
                        .title(artworkDto.getTitle())
                        .description(artworkDto.getDescription())
                        .originFileName(artworkDto.getOriginFileName())
                        .saveFileName(artworkDto.getSaveFileName())
                        .saveFolder(artworkDto.getSaveFolder())
                        .build()
        );

        if(artwork==null) return false; // ?????? ??????

        return true;
    }

    @Transactional
    public Optional<Artwork> update(Long id, ArtworkUpdateRequestDto requestDto){
        Optional<Artwork> artwork = artworkRepository.findById(id); // ????????? ?????? ??????

        // ????????? ?????? ??????
        artwork.get().setTitle(requestDto.getTitle());
        artwork.get().setDescription(requestDto.getDescription());
        artwork.get().setOriginFileName(requestDto.getOriginFileName());
        artwork.get().setSaveFileName(requestDto.getSaveFileName());
        artwork.get().setSaveFolder(requestDto.getSaveFolder());

        return artwork;
    }

    // id??? ???????????? ????????? ????????? ????????? ???????????? ???????????? ????????????
    public String getParentFile(Long id){

        Artwork artwork=artworkRepository.findById(id).get();
        String saveFolder=artwork.getSaveFolder();

        return saveFolder;
    }

    // id??? ???????????? ????????? ?????? ????????? ????????????
    public ArtworkResponseDto findByNo(Long id, Long loginMemberId){

        Artwork artwork=artworkRepository.findById(id).get(); // id??? ???????????? ??????
        Member writer=artwork.getWriter(); // ??????
        boolean isLike=(likeRepository.selectIsLike(id, loginMemberId)>0)?true:false;
        boolean isFollow=(followRepository.isFollow(loginMemberId, writer.getId())>0)?true:false;

        ArtworkResponseDto response = ArtworkResponseDto.builder()
                .writerId(writer.getId())
                .writerNickname(writer.getNickname())
                .title(artwork.getTitle())
                .description(artwork.getDescription())
                .regdate(artwork.getRegdate())
                .artworkSaveFolder(artworkImgUrl+artwork.getSaveFileName())
                .writerProfileSaveFolder(profileImgUrl+writer.getSaveFilename())
                .writerEmail(writer.getEmail())
                .likeNum(likeRepository.selectLikeNumByArtworkId(artwork.getId()))
                .likeOrNot(isLike)
                .followOrNot(isFollow)
                .build();

        return response;
    }

    // id??? ???????????? ?????? ????????? ?????? ????????? ????????????
    public List<ArtworkListResponseDto> getArtworkByMemberId(String nickname){
        Member member=memberRepository.findMemberByNickname(nickname); // ???????????? ???????????? ?????? ????????????

        if(member == null) return null; // ?????? ????????? ???????????? ??????

        List<Artwork> artworkList=member.getArtworkList(); // ????????? ?????? ????????? ????????????
        List<ArtworkListResponseDto> result = new ArrayList<>();

        for(Artwork artwork: artworkList){
            ArtworkListResponseDto response = ArtworkListResponseDto.builder()
                    .memberId(member.getId())
                    .memberNickname(member.getNickname())
                    .artworkId(artwork.getId())
                    .artworkTitle(artwork.getTitle())
                    .artworkRegdate(artwork.getRegdate())
                    .saveFileName(artwork.getSaveFileName())
                    .saveFolder(artwork.getSaveFolder())
                    .imageUrl(artworkImgUrl+artwork.getSaveFileName())
                    .build();
            result.add(response); // ???????????? ??????
        }

        Collections.sort(result);

        return result;

    }

    @Transactional
    public void delete(Long id){
        Artwork artwork=artworkRepository.findById(id).get();
        artworkRepository.delete(artwork);
    }

    public List<LikeArtworkResponseDto> getLikeArtworkList(String nickname){

        Member member = memberRepository.findMemberByNickname(nickname);
        List<Likes> likes = likeRepository.selectByMemberId(member.getId());

        if(likes.isEmpty()) return null; // ???????????? ????????? ?????? ????????? null ??????

        List<LikeArtworkResponseDto> result=new ArrayList<>();

        for(Likes like: likes){
            Artwork artwork = like.getArtwork(); // ????????? ??? ??????
            Member writer = artwork.getWriter(); // ????????? ??? ????????? ?????????

            LikeArtworkResponseDto response = LikeArtworkResponseDto.builder()
                    .artworkTitle(artwork.getTitle())
                    .memberNickname(writer.getNickname())
                    .memberId(writer.getId())
                    .saveFolder(artworkImgUrl+artwork.getSaveFileName())
                    .likeOrNot(true)
                    .artworkId(artwork.getId())
                    .regdate(artwork.getRegdate())
                    .build();

            result.add(response);
        }

        Collections.sort(result);
        return result;
    }// end getLikeArtworkList

    // ???????????? ????????? ?????? ?????????
    public List<FollowArtworkListResponseDto> selectFollowArtworkList(String nickname) {

        Member member=memberRepository.findMemberByNickname(nickname); // nickname??? ???????????? ??????
        List<Follow> followList=followRepository.findAllFollowing(member.getId()); // nickname??? ??????????????? ????????? ?????????
        List<FollowArtworkListResponseDto> result=new ArrayList<>(); // ???????????? ????????? ?????? ???????????? ?????? ?????????

        if(followList==null) return null; // ??????????????? ????????? ????????? null ??????

        for(Follow follow: followList){ // ??????????????? ????????? ??????????????? ?????????
            Member followee = follow.getFollowee(); // ??????????????? ??????
            List<Artwork> artworks=artworkRepository.find4ByMemberId(followee.getId()); // 4??? ?????? ????????????
            List<SimpleArtworkDto> artworkInfo=new ArrayList<>();

            LocalDateTime recentUpdated=null;
            if(!artworks.isEmpty()){ // ????????? ???????????????
                recentUpdated=artworks.get(0).getRegdate(); // ?????? ????????? ????????? ??????
                for(Artwork artwork: artworks){ // ???????????? ?????????
                    artworkInfo.add(SimpleArtworkDto.builder()
                            .artworkId(artwork.getId())
                            .saveFolder(artworkImgUrl+artwork.getSaveFileName())
                            .build()); // ?????? ???????????? ?????? url ??????
                }
            }

            FollowArtworkListResponseDto response = FollowArtworkListResponseDto.builder()
                    .artworkInfo(artworkInfo)
                    .artworkNum(artworkRepository.findNumByMemberId(followee.getId()))
                    .followerNum(followRepository.findAllFollowedCnt(followee.getId()))
                    .memberMail(followee.getEmail())
                    .recentUpdated(recentUpdated)
                    .build();

            result.add(response);
        }

        Collections.sort(result);
        return result;
    }

}
