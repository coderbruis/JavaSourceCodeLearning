package com.bruis.learnsb.bean;

/**
 * @author LuoHaiYang
 */
public class MyRegistryBean extends Person{
    @Override
    public String getName() {
        return "myRegistyBean";
    }

    @Override
    public void setName(String name) {
        super.setName(name);
    }

    @Override
    public int getAge() {
        return 20;
    }

    @Override
    public void setAge(int age) {
        super.setAge(age);
    }

    @Override
    public String getPhone() {
        return "18483223866";
    }

    @Override
    public void setPhone(String phone) {
        super.setPhone(phone);
    }
}
