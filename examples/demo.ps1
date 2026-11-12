param(
    [string]$BaseUrl = 'http://localhost:8080'
)

$ErrorActionPreference = 'Stop'
$suffix = [guid]::NewGuid().ToString('N')

function Send-Request {
    param([string]$Method, [string]$Path, $Body)
    $params = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
    }
    if ($null -ne $Body) {
        $params.ContentType = 'application/json; charset=utf-8'
        $params.Body = [System.Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 10))
    }
    Invoke-RestMethod @params
}

Write-Host '1. Создаём инструктора и студента'
$instructor = Send-Request POST '/api/instructors' @{
    firstName = 'Анна'; lastName = 'Петрова'; email = "teacher.$suffix@example.com"; bio = 'Преподаю Java'
}
$student = Send-Request POST '/api/students' @{
    firstName = 'Иван'; lastName = 'Иванов'; email = "student.$suffix@example.com"
}

Write-Host '2. Создаём черновик курса и два урока'
$course = Send-Request POST '/api/courses' @{
    title = 'Основы Java'; description = 'Учебный пример'; instructorId = $instructor.id
}
$text = Send-Request POST "/api/courses/$($course.id)/lessons" @{
    title = 'Введение'; type = 'TEXT'; content = '# Java — объектно-ориентированный язык'; readingTimeMinutes = 5
}
$quiz = Send-Request POST "/api/courses/$($course.id)/lessons" @{
    title = 'Проверка знаний'; type = 'QUIZ'; passingScore = 70
    questions = @(@{ text = 'Сколько будет 2 + 2?'; options = @('3', '4', '5'); correctOptionIndex = 1 })
}

Write-Host '3. Публикуем курс и записываем студента'
$null = Send-Request PATCH "/api/courses/$($course.id)/publish"
$enrollment = Send-Request POST '/api/enrollments' @{ studentId = $student.id; courseId = $course.id }

Write-Host '4. Завершаем текстовый урок'
$progress = Send-Request POST "/api/enrollments/$($enrollment.id)/lessons/$($text.id)/complete"
Write-Host "Прогресс: $($progress.progressPercent)%"

Write-Host '5. Получаем вопросы без правильных ответов и сдаём тест'
$questions = Send-Request GET "/api/lessons/$($quiz.id)/questions"
$result = Send-Request POST "/api/lessons/$($quiz.id)/submit" @{
    enrollmentId = $enrollment.id
    answers = @(@{ questionId = $questions[0].id; selectedOption = 1 })
}
Write-Host "Балл: $($result.scorePercent)%; тест пройден: $($result.passed)"

Write-Host '6. Проверяем завершение и скачиваем сертификат'
$progress = Send-Request GET "/api/enrollments/$($enrollment.id)/progress"
if ($progress.status -ne 'COMPLETED' -or $progress.progressPercent -ne 100) {
    throw 'Ожидалось завершение курса с прогрессом 100%'
}
$certificates = @(Send-Request GET "/api/students/$($student.id)/certificates")
if ($certificates.Count -ne 1) {
    throw 'Ожидался один сертификат'
}
$certificate = $certificates[0]
$verified = Send-Request GET "/api/certificates/$($certificate.uniqueCode)"
if ($verified.id -ne $certificate.id) {
    throw 'Проверка сертификата вернула другой ID'
}
$outputDir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../target'))
$null = New-Item -ItemType Directory -Path $outputDir -Force
$outputFile = Join-Path $outputDir 'demo-certificate.txt'
Invoke-WebRequest -Uri "$BaseUrl/api/certificates/$($certificate.id)/download" -OutFile $outputFile -UseBasicParsing
Write-Host "Готово. Сертификат: $outputFile"
Write-Host "Курс: $($course.id), запись: $($enrollment.id), код: $($certificate.uniqueCode)"
