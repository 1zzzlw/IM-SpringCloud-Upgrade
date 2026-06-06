package com.zzzlew.controller;

import com.zzzlew.domain.dto.FavoritesDTO;
import com.zzzlew.domain.vo.FavoritesVO;
import com.zzzlew.result.Result;
import com.zzzlew.server.FavoritesService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2026/2/21 - 02 - 21 - 21:03
 * @Description: com.zzzlew.controller
 * @version: 1.0
 */
@Slf4j
@RestController
@RequestMapping("/favorites")
@Tag(name = "收藏接口")
public class FavoritesController {

    @Resource
    private FavoritesService favoritesService;

    /**
     * 上传文本中的图片
     *
     * @param images
     * @return
     */
    @PostMapping("/uploadImage")
    public Result<List<String>> uploadImage(@RequestParam("images") List<MultipartFile> images) {
        log.info("上传图片：{}", images);
        List<String> urlList = favoritesService.uploadImage(images);
        return Result.success(urlList);
    }

    /**
     * 保存笔记
     *
     * @param favoritesDTO
     * @return
     */
    @PostMapping("/saveNote")
    public Result<Object> saveNote(@RequestBody FavoritesDTO favoritesDTO) {
        log.info("保存笔记：{}", favoritesDTO);
        favoritesService.saveNote(favoritesDTO);
        return Result.success();
    }

    /**
     * 更新笔记
     *
     * @param favoritesDTO
     * @return
     */
    @PostMapping("/updateNote")
    public Result<Object> updateNote(@RequestBody FavoritesDTO favoritesDTO) {
        log.info("更新笔记：{}", favoritesDTO);
        favoritesService.updateNote(favoritesDTO);
        return Result.success();
    }

    @GetMapping("/getNote")
    public Result<List<FavoritesVO>> getNote() {
        List<FavoritesVO> favoritesVOList = favoritesService.getNote();
        return Result.success(favoritesVOList);
    }
}
