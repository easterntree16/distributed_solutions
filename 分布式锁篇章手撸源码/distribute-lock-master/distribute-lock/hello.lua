print("hello world")
--数据结构
print(type(nil))
print(type(false))
print(type(1))
print(type(1.1))
print(type(type))
print(type("hello world"))
print(type({"2", "3"}))

--变量
a = 5
print(a)
print(c)
local b = 4
print(b)

do
    local x = 1
    c = 7
    print(x)
    print(c)
end
print(x)
print(c)

--多变量赋值
a, b, c = 2, 3
print(a .. b)
print(c)

a, b = 2, 3, 4
print(a .. b)

--table赋值
tal = {key = "a", key1 = "b"}
print(tal["key"])
tal["key"] = "c"
print(tal["key"])
print(tal["key1"])

a = 1
while(a < 5) do
    print(a)
    a = a + 1
end

for i=1, 5, 1 do
    print(i)
end

for i=5, 1, -1 do
    print(i)
end

--流程控制
a = 9
if(a < 10) then
    print("小于10")
end

a = 10
if(a < 10) then
    print("小于10")
else
    print("大于等于10")
end

a = 10
if(a < 10) then
    print("小于10")
elseif(a == 10) then
    print("等于10")
else
    print("小于10")
end

--函数
function xx(a)
    print(a)
end
xx(10)

xxx = function (a)
    print("哈哈哈" .. a)
end
function sum (a, b, func1)
    result = a + b
    func1(result)
end

sum(1, 2 , xxx)

print(#"hello")