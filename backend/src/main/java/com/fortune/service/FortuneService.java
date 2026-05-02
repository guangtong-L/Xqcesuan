package com.fortune.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.fortune.common.BizException;
import com.fortune.common.ErrorCode;
import com.fortune.dto.req.FortuneCalcReq;
import com.fortune.dto.req.UnlockReq;
import com.fortune.dto.resp.DimensionVO;
import com.fortune.dto.resp.FortuneResp;
import com.fortune.dto.resp.LuckyVO;
import com.fortune.dto.resp.UnlockResp;
import com.fortune.mock.MockDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class FortuneService {

    private final MockDataService mockDataService;
    private final HistoryService historyService;

    private final AtomicLong totalCalcCounter = new AtomicLong();
    private final ConcurrentHashMap<String, Boolean> uniqueUserToday = new ConcurrentHashMap<>();
    private final AtomicLong unlockCounter = new AtomicLong();
    private volatile String statsDate = todayStr();

    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter REC_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final String TIPS = "本结果基于趣味算法生成，仅供娱乐参考，不构成任何医疗、投资、婚恋等决策建议。";

    public FortuneResp calc(FortuneCalcReq req, String openid) {
        if (StrUtil.isBlank(openid)) {
            throw new BizException(ErrorCode.NOT_LOGIN);
        }
        LocalDate birth = parseBirthday(req.getBirthday());
        rolloverStatsIfNeeded();

        String today = todayStr();
        byte[] seed = DigestUtil.md5(openid + req.getBirthday() + req.getGender() + today);

        int score = (intFromBytes(seed, 0, 4) & 0x7FFFFFFF) % 100 + 1;
        String levelKey = MockDataService.levelKeyByScore(score);
        String levelDisplay = MockDataService.levelDisplayByScore(score);

        int loveScore = scoreSeg(seed, 4, 6);
        int careerScore = scoreSeg(seed, 6, 8);
        int wealthScore = scoreSeg(seed, 8, 10);
        int healthScore = scoreSeg(seed, 10, 12);

        List<DimensionVO> dimensions = new ArrayList<>(4);
        dimensions.add(buildDim("love", "爱情", loveScore, intFromBytes(seed, 4, 6)));
        dimensions.add(buildDim("career", "事业", careerScore, intFromBytes(seed, 6, 8)));
        dimensions.add(buildDim("wealth", "财运", wealthScore, intFromBytes(seed, 8, 10)));
        dimensions.add(buildDim("health", "健康", healthScore, intFromBytes(seed, 10, 12)));

        String summary = mockDataService.pickFortuneText(levelKey, "career", intFromBytes(seed, 12, 14));
        LuckyVO lucky = mockDataService.pickLucky(intFromBytes(seed, 12, 16));
        List<List<String>> yj = mockDataService.pickYiJi(intFromBytes(seed, 0, 4));

        String zodiac = zodiacOf(birth);
        String shortHash = DigestUtil.md5Hex(openid).substring(0, 8);
        String recordId = "rec_" + LocalDate.now().format(REC_FMT) + "_" + shortHash;

        FortuneResp resp = FortuneResp.builder()
                .recordId(recordId)
                .date(today)
                .zodiac(zodiac)
                .score(score)
                .level(levelDisplay)
                .summary(summary)
                .dimensions(dimensions)
                .yi(yj.get(0))
                .ji(yj.get(1))
                .lucky(lucky)
                .hasDeepContent(true)
                .unlocked(false)
                .tips(TIPS)
                .createdAt(OffsetDateTime.now(ZoneOffset.of("+08:00")))
                .build();

        historyService.save(openid, resp);

        totalCalcCounter.incrementAndGet();
        uniqueUserToday.putIfAbsent(openid, Boolean.TRUE);

        return resp;
    }

    private DimensionVO buildDim(String key, String name, int score, int seed) {
        String text = mockDataService.pickFortuneText(MockDataService.levelKeyByScore(score), key, seed);
        return DimensionVO.builder().key(key).name(name).score(score).text(text).build();
    }

    public UnlockResp unlock(UnlockReq req, String openid) {
        if (StrUtil.isBlank(openid)) {
            throw new BizException(ErrorCode.NOT_LOGIN);
        }
        FortuneResp existed = historyService.getOne(openid, req.getRecordId());
        if (Boolean.TRUE.equals(existed.getUnlocked()) && existed.getDeepContent() != null) {
            return UnlockResp.builder()
                    .recordId(existed.getRecordId())
                    .unlocked(true)
                    .deepContent(existed.getDeepContent())
                    .build();
        }
        existed.setUnlocked(true);
        existed.setDeepContent(mockDataService.cloneDeepContent());
        unlockCounter.incrementAndGet();
        return UnlockResp.builder()
                .recordId(existed.getRecordId())
                .unlocked(true)
                .deepContent(existed.getDeepContent())
                .build();
    }

    private LocalDate parseBirthday(String s) {
        try {
            LocalDate d = LocalDate.parse(s, D_FMT);
            if (d.isAfter(LocalDate.now())) {
                throw new BizException(ErrorCode.PARAM_RANGE, "生日不能晚于今天");
            }
            if (d.isBefore(LocalDate.of(1900, 1, 1))) {
                throw new BizException(ErrorCode.PARAM_RANGE, "生日早于 1900-01-01");
            }
            return d;
        } catch (DateTimeParseException e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "生日格式错误");
        }
    }

    private int scoreSeg(byte[] seed, int from, int to) {
        return (intFromBytes(seed, from, to) & 0x7FFFFFFF) % 100 + 1;
    }

    private int intFromBytes(byte[] bytes, int from, int to) {
        int v = 0;
        int end = Math.min(to, Math.min(from + 4, bytes.length));
        for (int i = from; i < end; i++) {
            v = (v << 8) | (bytes[i] & 0xFF);
        }
        return v;
    }

    private static String todayStr() {
        return LocalDate.now().format(D_FMT);
    }

    private synchronized void rolloverStatsIfNeeded() {
        String today = todayStr();
        if (!today.equals(statsDate)) {
            log.info("[stats] 跨天 {} -> {}, 清理今日唯一用户集合", statsDate, today);
            uniqueUserToday.clear();
            statsDate = today;
        }
    }

    public static String zodiacOf(LocalDate d) {
        int m = d.getMonthValue();
        int day = d.getDayOfMonth();
        String[] names = {
                "摩羯座","水瓶座","双鱼座","白羊座","金牛座","双子座",
                "巨蟹座","狮子座","处女座","天秤座","天蝎座","射手座","摩羯座"
        };
        int[] starts = {20,19,21,20,21,22,23,23,23,24,23,22};
        int idx = (day < starts[m - 1]) ? m - 1 : m;
        return names[idx];
    }

    public long getTotalCalc() { return totalCalcCounter.get(); }
    public long getUniqueUserToday() {
        rolloverStatsIfNeeded();
        return uniqueUserToday.size();
    }
    public long getUnlockCount() { return unlockCounter.get(); }
    public String getStatsDate() {
        rolloverStatsIfNeeded();
        return statsDate;
    }

    public void resetCountersForTest() {
        totalCalcCounter.set(0);
        unlockCounter.set(0);
        uniqueUserToday.clear();
    }
}
