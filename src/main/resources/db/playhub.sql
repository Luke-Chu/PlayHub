create table shop
(
    id          bigint unsigned auto_increment comment '主键'
        primary key,
    name        varchar(10)                         not null comment '商家名称',
    create_time timestamp default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time timestamp default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '商家表';

create table user
(
    id          bigint unsigned auto_increment comment '主键'
        primary key,
    name        varchar(10)                         not null comment '用户名',
    create_time timestamp default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time timestamp default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '用户表';

create table voucher
(
    id          bigint unsigned auto_increment comment '主键'
        primary key,
    shop_id     int unsigned                        not null comment '商铺id',
    stock       int unsigned                        not null comment '库存',
    create_time timestamp default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time timestamp default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '优惠券表';

create table voucher_order
(
    id          bigint unsigned                     not null comment '主键'
        primary key,
    user_id     int unsigned                        not null comment '下单用户id',
    voucher_id  int unsigned                        not null comment '优惠券id',
    create_time timestamp default CURRENT_TIMESTAMP not null comment '下单时间',
    update_time timestamp default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '优惠券秒杀订单';

