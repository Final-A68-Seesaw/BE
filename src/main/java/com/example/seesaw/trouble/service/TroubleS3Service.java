package com.example.seesaw.trouble.service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.example.seesaw.trouble.model.TroubleImage;
import com.example.seesaw.trouble.repository.TroubleImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TroubleS3Service {

    private AmazonS3 s3Client;

    @Value("${cloud.aws.credentials.accessKey}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secretKey}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    private final String bucket = "myseesaw";

    private final TroubleImageRepository troubleImageRepository;

    @PostConstruct
    public void setS3Client() {
        AWSCredentials credentials = new BasicAWSCredentials(this.accessKey, this.secretKey);

        s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(this.region)
                .build();
    }

    // 최초 게시글 작성
    public List<String> upload(List<MultipartFile> files) {

        List<String> imageUrls = new ArrayList<>();
        for(MultipartFile file : files){
            String fileName = createFileName(file.getOriginalFilename());
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(file.getSize());
            objectMetadata.setContentType(file.getContentType());

            try (InputStream inputStream = file.getInputStream()) {
                s3Client.putObject(new PutObjectRequest(bucket, fileName, inputStream, objectMetadata)
                        .withCannedAcl(CannedAccessControlList.PublicRead));
                imageUrls.add(s3Client.getUrl(bucket, fileName).toString());
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패하셨습니다");
            }
        }

        return imageUrls;
    }


    // 글 수정(+ 기존 s3에 있는 이미지 정보 삭제)
    public List<String> update(Long troubleId, List<String> imageUrls, List<MultipartFile> files) {
        //이미지 삭제 후 재업로드
        delete(troubleId, imageUrls);
        return upload(files);
    }

    //기존 s3에 있는 기존 이미지 정보, DB 정보 삭제
    public void delete(Long troubleId, List<String> imageUrls) {   //imageUrls 는 지우면 안되는 것들
        List<TroubleImage> savedImages = troubleImageRepository.findAllByTroubleId(troubleId);

        //살리고 싶은 이미지에 기존에 저장된 이미지가 포함되어 있다면, 지워야할 이미지에서 지워라!
        savedImages.removeIf(savedImage -> imageUrls != null && imageUrls.contains(savedImage.getTroubleImage()));

        for (TroubleImage savedImage : savedImages) {
            if (!savedImage.getTroubleImage().equals("https://myseesaw.s3.ap-northeast-2.amazonaws.com/TroubleBaicCard.svg")) {
                String image = savedImage.getTroubleImage().replace("https://myseesaw.s3.ap-northeast-2.amazonaws.com/", "");
                boolean isExistObject = s3Client.doesObjectExist(bucket, image);
                System.out.println("지워야할 url 주소 : " + image);
                System.out.println("isExistObject : " + isExistObject);
                if (isExistObject) {
                    s3Client.deleteObject(bucket, image);
                }
            }
            System.out.println(savedImage.getTroubleImage());
            troubleImageRepository.deleteById(savedImage.getId());
        }
    }


    private String createFileName(String fileName) {
        // 먼저 파일 업로드 시, 파일명을 난수화하기 위해 random으로 돌립니다.
        return UUID.randomUUID().toString().concat(getFileExtension(fileName));
    }

    private String getFileExtension(String fileName) {
        // file 형식이 잘못된 경우를 확인하기 위해 만들어진 로직이며,
        // 파일 타입과 상관없이 업로드할 수 있게 하기 위해 .의 존재 유무만 판단
        System.out.println("파일이름알고싶어요!! : " + fileName.substring(fileName.lastIndexOf(".")));
        try {
            return fileName.substring(fileName.lastIndexOf("."));
        } catch (StringIndexOutOfBoundsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 형식의 파일(" + fileName + ") 입니다.");
        }
    }
}