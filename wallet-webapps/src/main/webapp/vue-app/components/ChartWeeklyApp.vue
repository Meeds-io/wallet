


<script>
import VueCharts from 'vue-chartjs'
import { line } from 'vue-chartjs'


export default {
  extends: Line,
  props: ['data', 'options'],
  mounted () {
    this.renderChart(this.data, this.options)
  }
}
</script>




