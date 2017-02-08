require 'net/http'

@host = 'localhost'
@port = '8080'
#@host = '83.212.123.149'
#@host = '83.212.113.64'
#@port = '8080'
#@host = 'optimum-recommender.imu-projects.eu'
#@port = '8080'

@path = "/recommender/route_recommender"

@body = ""

#file = File.new("body_14_07_2013.txt", "r")
#file = File.new("body_14_07_2013.txt", "r")
#file = File.new("body_23_06_2014.txt", "r")
#file = File.new("dublin_route.txt", "r")
#file = File.new("body_23_06_2014.txt", "r")
file = File.new("error_request.json", "r")
while (line = file.gets)
    @body = @body + "#{line}"    
end
file.close

#puts @body

request = Net::HTTP::Post.new(@path, initheader = {'Content-Type' =>'text/plain;charset=utf-8'})
request.initialize_http_header({"X-USER-ID" => "XcwoeOk7RCcN9wilopVGt82nas7GdAG8"})
request.body = @body
response = Net::HTTP.new(@host, @port).start {|http| http.request(request) }
puts "Response #{response.code} #{response.message}: #{response.body}"

file = File.open("response.txt", 'w') { |file| file.write("Response #{response.code} #{response.message}: #{response.body}") }
#file.close