package com.fortune.service;

import cn.hutool.core.util.StrUtil;
import com.fortune.common.BizException;
import com.fortune.common.ErrorCode;
import com.fortune.dto.resp.FortuneResp;
import com.fortune.dto.resp.HistoryItemVO;
import com.fortune.dto.resp.HistoryListResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 历史记录（内存）
 * <p>
 * <strong>线上风险</strong>：
 * 1) 内存存储，重启即丢，需提示用户；
 * 2) 必须做 openid 归属校验，避免越权；
 * 3) 切 DB 时改"软删除" + 加唯一索引(openid,recordId)。
 */
@Slf4j
@Service
public class HistoryService {

    /** key=openid，value=该用户的记录列表（最新在前） */
    private final ConcurrentHashMap<String, LinkedList<FortuneResp>> store = new ConcurrentHashMap<>();

    @Value("${fortune.history.max-per-user:30}")
    private int maxPerUser;

    /**
     * 写入或覆盖一条记录（同 recordId 视为同条覆盖）
     */
    public void save(String openid, FortuneResp record) {
        if (StrUtil.isBlank(openid) || record == null) {
            return;
        }
        store.compute(openid, (k, list) -> {
            if (list == null) {
                list = new LinkedList<>();
            }
            // 同 recordId 去重（按时段内重复测算覆盖）
            list.removeIf(r -> r.getRecordId().equals(record.getRecordId()));
            list.addFirst(record);
            // 超出上限剔除最旧
            while (list.size() > maxPerUser) {
                list.removeLast();
            }
            return list;
        });
    }

    /**
     * 分页查询历史
     */
    public HistoryListResp page(String openid, int page, int pageSize) {
        LinkedList<FortuneResp> list = store.getOrDefault(openid, new LinkedList<>());
        List<FortuneResp> snapshot;
        synchronized (list) { // 防止遍历时被并发修改
            snapshot = new ArrayList<>(list);
        }
        int total = snapshot.size();
        int from = Math.min((page - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        List<HistoryItemVO> items = snapshot.subList(from, to).stream()
                .map(this::toItem)
                .collect(Collectors.toList());

        return HistoryListResp.builder()
                .page(page)
                .pageSize(pageSize)
                .total(total)
                .list(items)
                .build();
    }

    /**
     * 获取单条记录（含归属校验）。不存在或不属于当前用户都抛 RECORD_NOT_FOUND
     */
    public FortuneResp getOne(String openid, String recordId) {
        LinkedList<FortuneResp> list = store.get(openid);
        if (list == null) {
            throw new BizException(ErrorCode.RECORD_NOT_FOUND);
        }
        synchronized (list) {
            for (FortuneResp r : list) {
                if (r.getRecordId().equals(recordId)) {
                    return r;
                }
            }
        }
        throw new BizException(ErrorCode.RECORD_NOT_FOUND);
    }

    /**
     * 删除单条。归属校验失败统一返回 RECORD_NOT_FOUND，避免 id 枚举攻击
     */
    public int deleteOne(String openid, String recordId) {
        LinkedList<FortuneResp> list = store.get(openid);
        if (list == null || list.isEmpty()) {
            throw new BizException(ErrorCode.RECORD_NOT_FOUND);
        }
        boolean removed;
        synchronized (list) {
            removed = list.removeIf(r -> r.getRecordId().equals(recordId));
        }
        if (!removed) {
            throw new BizException(ErrorCode.RECORD_NOT_FOUND);
        }
        return 1;
    }

    /**
     * 全部删除（按用户隔离）
     */
    public int deleteAll(String openid) {
        LinkedList<FortuneResp> list = store.remove(openid);
        return list == null ? 0 : list.size();
    }

    /**
     * 标记解锁（落地到内存记录）
     */
    public FortuneResp markUnlocked(String openid, String recordId, FortuneResp.FortuneRespBuilder mutator) {
        FortuneResp existed = getOne(openid, recordId);
        existed.setUnlocked(true);
        return existed;
    }

    private HistoryItemVO toItem(FortuneResp r) {
        return HistoryItemVO.builder()
                .recordId(r.getRecordId())
                .date(r.getDate())
                .zodiac(r.getZodiac())
                .score(r.getScore())
                .level(r.getLevel())
                .summary(r.getSummary())
                .createdAt(r.getCreatedAt())
                .build();
    }

    // 仅用于单测：清空全部
    public void clearAllForTest() {
        store.clear();
    }

    public List<FortuneResp> snapshotForTest(String openid) {
        LinkedList<FortuneResp> list = store.get(openid);
        if (list == null) return Collections.emptyList();
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }
}
