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
    public Result<Long> saveNote(@RequestBody FavoritesDTO favoritesDTO) {
        log.info("保存笔记：{}", favoritesDTO);
        Long id = favoritesService.saveNote(favoritesDTO);
        return Result.success(id);
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

    /**
     * 保存收藏（支持所有类型：文本、图片、视频、文件等）
     *
     * @param favoritesDTO 收藏信息
     * @return 操作结果
     */
    @PostMapping("/save")
    public Result<Object> saveFavorite(@RequestBody FavoritesDTO favoritesDTO) {
        log.info("保存收藏：{}", favoritesDTO);
        favoritesService.saveFavorite(favoritesDTO);
        return Result.success();
    }

    /**
     * 获取当前用户所有收藏（包含所有类型）
     *
     * @return 收藏列表
     */
    @GetMapping("/list")
    public Result<List<FavoritesVO>> listFavorites() {
        List<FavoritesVO> favoritesVOList = favoritesService.getAllFavorites();
        return Result.success(favoritesVOList);
    }

    /**
     * 删除收藏
     *
     * @param id 收藏ID
     * @return 操作结果
     */
    @DeleteMapping("/delete/{id}")
    public Result<Object> deleteFavorite(@PathVariable Long id) {
        log.info("删除收藏：{}", id);
        favoritesService.deleteFavorite(id);
        return Result.success();
    }
}
