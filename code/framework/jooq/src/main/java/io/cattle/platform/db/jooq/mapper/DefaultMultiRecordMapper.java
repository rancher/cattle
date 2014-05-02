package io.cattle.platform.db.jooq.mapper;

import java.util.List;

public class DefaultMultiRecordMapper extends MultiRecordMapper<List<Object>> {

    @Override
    protected List<Object> map(List<Object> input) {
        return input;
    }

}
