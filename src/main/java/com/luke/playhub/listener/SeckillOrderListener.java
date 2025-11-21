package com.luke.playhub.listener;

import com.luke.playhub.entity.VoucherOrder;
import com.luke.playhub.mapper.VoucherMapper;
import com.luke.playhub.service.VoucherOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Luke
 * @since 2025/11/21 21:21
 */

@RequiredArgsConstructor
@Component
public class SeckillOrderListener {
    private final VoucherOrderService voucherOrderService;
    private final VoucherMapper voucherMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = "seckill.order.queue")
    public void listenSeckillOrderQueue(VoucherOrder voucherOrder) {
        try {
            // 1. 再次检查订单是否已存在（幂等，避免重复消费）
            if (voucherOrderService.exists(voucherOrder.getId())) {
                return;
            }

            // 2. MySQL 扣减库存
            int changed = voucherMapper.decreaseStockGreaterZero(voucherOrder.getVoucherId());
            if (changed == 0) {
                // 补偿逻辑：Redis库存回滚
                stringRedisTemplate.opsForValue().increment("stock:voucher:" + voucherOrder.getVoucherId(), 1);
                stringRedisTemplate.opsForSet().remove("order:voucher:" + voucherOrder.getVoucherId(), voucherOrder.getUserId());
                return;
            }

            // 3. 创建订单
            voucherOrderService.create(voucherOrder);
        } catch (Exception e) {
            // MQ失败重试，或手动补偿
            throw new RuntimeException(e);
        }
    }
}
